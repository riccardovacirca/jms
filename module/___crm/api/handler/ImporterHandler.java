package dev.jms.app.crm.handler;

import dev.jms.app.crm.dao.ContattoDAO;
import dev.jms.app.crm.dao.ImportSessionDAO;
import dev.jms.app.crm.dao.ListaDAO;
import dev.jms.app.crm.dto.ContattoDTO;
import dev.jms.app.crm.dto.ImportSessionDTO;
import dev.jms.app.crm.dto.ListaDTO;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Excel;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler per le operazioni di importazione contatti da file Excel (entità importer).
 */
public class ImporterHandler
{
  private static final Log log = Log.get(ImporterHandler.class);
  private static final String DEFAULT_TMP_DIR = "/app/storage/crm/tmp";

  private final String tmpDir;

  /** Costruttore. Legge il path di storage temporaneo dalla configurazione. */
  public ImporterHandler(Config config)
  {
    this.tmpDir = config.get("crm.resources.tmp", DEFAULT_TMP_DIR);
  }

  /**
   * GET /api/import/campi — elenco dei campi importabili del sistema.
   */
  public void campi(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    List<HashMap<String, String>> result;

    session.require(Role.ADMIN, Permission.READ);
    result = new ArrayList<>();
    result.add(campo("nome",            "Nome"));
    result.add(campo("cognome",         "Cognome"));
    result.add(campo("ragione_sociale", "Ragione Sociale"));
    result.add(campo("telefono",        "Telefono"));
    result.add(campo("email",           "Email"));
    result.add(campo("indirizzo",       "Indirizzo"));
    result.add(campo("citta",           "Città"));
    result.add(campo("cap",             "CAP"));
    result.add(campo("provincia",       "Provincia"));
    result.add(campo("note",            "Note"));
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(result)
       .send();
  }

  /**
   * POST /api/import/analyze — upload file Excel, analizza e crea sessione di importazione.
   */
  public void analyze(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    byte[] fileBytes;
    String filename;
    Excel.AnalysisResult analysis;
    String analysisError;
    String sessionId;
    Path tmpFile;
    ImportSessionDAO dao;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    fileBytes = req.getMultipartFileBytes("file");
    if (fileBytes == null || fileBytes.length == 0) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Nessun file ricevuto")
         .out(null)
         .send();
    } else {
      filename = req.getMultipartFilename("file");
      if (filename == null) {
        filename = "import.xlsx";
      }
      analysisError = null;
      analysis      = null;
      try {
        analysis = Excel.analyze(new ByteArrayInputStream(fileBytes), 5);
      } catch (Exception e) {
        log.warn("Errore analisi file: " + e.getMessage());
        analysisError = e.getMessage();
      }
      if (analysisError != null) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Errore nel file: " + analysisError)
           .out(null)
           .send();
      } else {
        tmpFile   = createTempFile(filename, fileBytes);
        sessionId = UUID.randomUUID().toString();
        dao       = new ImportSessionDAO(db);
        dao.create(
          sessionId,
          filename,
          tmpFile.toString(),
          analysis.totalRows,
          Json.encode(analysis.headers),
          Json.encode(analysis.previewRows)
        );
        out = new HashMap<>();
        out.put("sessionId", sessionId);
        out.put("filename",  filename);
        out.put("rowCount",  analysis.totalRows);
        out.put("headers",   analysis.headers);
        out.put("preview",   analysis.previewRows);
        out.put("warnings",  analysis.warnings);
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(out)
           .send();
      }
    }
  }

  /**
   * PUT /api/import/{id}/mapping — salva la mappatura colonne → campi. Body: {@code {"mapping": {...}}}.
   */
  @SuppressWarnings("unchecked")
  public void mapping(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO importSession;
    HashMap<String, Object> body;
    Object mappingObj;

    session.require(Role.ADMIN, Permission.WRITE);
    sessionId     = req.urlArgs().get("id");
    dao           = new ImportSessionDAO(db);
    importSession = dao.findById(sessionId);
    if (importSession == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Sessione non trovata")
         .out(null)
         .send();
    } else {
      body       = Json.decode(req.getBody(), HashMap.class);
      mappingObj = body.get("mapping");
      if (mappingObj == null) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Parametro 'mapping' mancante")
           .out(null)
           .send();
      } else {
        dao.updateMapping(sessionId, Json.encode(mappingObj));
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(null)
           .send();
      }
    }
  }

  /**
   * GET /api/import/{id}/validate — valida le righe del file con la mappatura salvata.
   */
  @SuppressWarnings("unchecked")
  public void validate(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO importSession;
    HashMap<String, Object> mapping;
    List<Map<String, Object>> rows;
    List<HashMap<String, Object>> result;
    Set<String> seenPhones;
    int validCount;
    int errorCount;
    int warningCount;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.READ);
    sessionId     = req.urlArgs().get("id");
    dao           = new ImportSessionDAO(db);
    importSession = dao.findById(sessionId);
    if (importSession == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Sessione non trovata")
         .out(null)
         .send();
    } else if (importSession.columnMapping() == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Mappatura colonne non configurata")
         .out(null)
         .send();
    } else if (!Files.exists(Paths.get(importSession.filePath()))) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("File di importazione non trovato")
         .out(null)
         .send();
    } else {
      mapping      = Json.decode(importSession.columnMapping(), HashMap.class);
      rows         = Excel.read(new FileInputStream(importSession.filePath()));
      result       = new ArrayList<>();
      seenPhones   = new HashSet<>();
      validCount   = 0;
      errorCount   = 0;
      warningCount = 0;
      for (Map<String, Object> row : rows) {
        HashMap<String, Object> mapped;
        List<String> errors;
        List<String> warnings;
        String nome;
        String cognome;
        String ragioneSociale;
        String telefono;
        HashMap<String, Object> r;
        mapped        = applyMapping(row, mapping);
        errors        = new ArrayList<>();
        warnings      = new ArrayList<>();
        nome          = stringify(mapped.get("nome"));
        cognome       = stringify(mapped.get("cognome"));
        ragioneSociale = stringify(mapped.get("ragione_sociale"));
        telefono      = normalize(stringify(mapped.get("telefono")));
        if (nome.isEmpty() && cognome.isEmpty() && ragioneSociale.isEmpty()) {
          errors.add("Identificatore mancante (nome, cognome o ragione sociale)");
        }
        if (telefono.isEmpty()) {
          warnings.add("Telefono mancante");
        } else if (seenPhones.contains(telefono)) {
          warnings.add("Telefono duplicato nel file");
        } else {
          seenPhones.add(telefono);
        }
        r = new HashMap<>();
        r.put("data",    mapped);
        r.put("errors",  errors);
        r.put("warnings", warnings);
        r.put("status",  errors.isEmpty() ? (warnings.isEmpty() ? "ok" : "warning") : "error");
        result.add(r);
        if (!errors.isEmpty()) {
          errorCount++;
        } else if (!warnings.isEmpty()) {
          warningCount++;
        } else {
          validCount++;
        }
      }
      out = new HashMap<>();
      out.put("valid",    validCount);
      out.put("errors",   errorCount);
      out.put("warnings", warningCount);
      out.put("rows",     result);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    }
  }

  /**
   * POST /api/import/{id}/execute — esegue l'importazione dei contatti nel database.
   */
  @SuppressWarnings("unchecked")
  public void execute(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO importSession;
    HashMap<String, Object> body;
    HashMap<String, Object> mapping;
    Integer listaId;
    boolean consenso;
    List<Map<String, Object>> rows;
    ContattoDAO contattoDao;
    ListaDAO listaDao;
    int importedCount;
    int skippedCount;
    int warningCount;
    boolean txFailed;
    String txError;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    sessionId     = req.urlArgs().get("id");
    dao           = new ImportSessionDAO(db);
    importSession = dao.findById(sessionId);
    if (importSession == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Sessione non trovata")
         .out(null)
         .send();
    } else if (importSession.columnMapping() == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Mappatura colonne non configurata")
         .out(null)
         .send();
    } else if (!Files.exists(Paths.get(importSession.filePath()))) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("File di importazione non trovato")
         .out(null)
         .send();
    } else {
      body    = Json.decode(req.getBody(), HashMap.class);
      listaId = body.get("listaId") != null ? ((Number) body.get("listaId")).intValue() : null;
      if (listaId == null) {
        ListaDAO defaultDao;
        ListaDTO defaultLista;
        defaultDao   = new ListaDAO(db);
        defaultLista = defaultDao.findDefault();
        if (defaultLista == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Nessuna lista di default configurata. Impostare una lista di default prima di importare.")
             .out(null)
             .send();
          listaId = null;
        } else {
          listaId = defaultLista.id();
        }
      }
      if (listaId != null) {
      consenso      = body.get("consenso") instanceof Boolean ? (Boolean) body.get("consenso") : false;
      mapping       = Json.decode(importSession.columnMapping(), HashMap.class);
      rows          = Excel.read(new FileInputStream(importSession.filePath()));
      contattoDao   = new ContattoDAO(db);
      listaDao      = new ListaDAO(db);
      importedCount = 0;
      skippedCount  = 0;
      warningCount  = 0;
      txFailed      = false;
      txError       = null;
      db.begin();
      try {
        for (Map<String, Object> row : rows) {
          HashMap<String, Object> mapped;
          String nome;
          String cognome;
          String ragioneSociale;
          String telefono;
          String email;
          String indirizzo;
          String citta;
          String cap;
          String provincia;
          String note;
          ContattoDTO c;
          int newId;
          mapped         = applyMapping(row, mapping);
          nome           = capitalize(stringify(mapped.get("nome")));
          cognome        = capitalize(stringify(mapped.get("cognome")));
          ragioneSociale = stringify(mapped.get("ragione_sociale"));
          telefono       = normalize(stringify(mapped.get("telefono")));
          email          = stringify(mapped.get("email")).toLowerCase();
          indirizzo      = stringify(mapped.get("indirizzo"));
          citta          = stringify(mapped.get("citta"));
          cap            = stringify(mapped.get("cap"));
          provincia      = stringify(mapped.get("provincia"));
          note           = stringify(mapped.get("note"));
          if (nome.isEmpty() && cognome.isEmpty() && ragioneSociale.isEmpty()) {
            skippedCount++;
            continue;
          }
          if (!telefono.isEmpty() && contattoDao.existsByTelefono(telefono, null)) {
            warningCount++;
            continue;
          }
          c = new ContattoDTO(
            null,
            nome.isEmpty()           ? null : nome,
            cognome.isEmpty()        ? null : cognome,
            ragioneSociale.isEmpty() ? null : ragioneSociale,
            telefono.isEmpty()       ? null : telefono,
            email.isEmpty()          ? null : email,
            indirizzo.isEmpty()      ? null : indirizzo,
            citta.isEmpty()          ? null : citta,
            cap.isEmpty()            ? null : cap,
            provincia.isEmpty()      ? null : provincia,
            note.isEmpty()           ? null : note,
            1,
            consenso,
            false,
            null,
            null,
            0L
          );
          newId = contattoDao.insert(c);
          listaDao.addContatto(listaId, newId);
          importedCount++;
        }
        db.commit();
      } catch (Exception e) {
        db.rollback();
        txFailed = true;
        txError  = e.getMessage();
      }
      if (txFailed) {
        dao.updateStatus(sessionId, "failed", txError);
        log.error("Errore durante importazione sessione " + sessionId + ": " + txError);
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Errore durante l'importazione")
           .out(null)
           .send();
      } else {
        dao.markCompleted(sessionId);
        out = new HashMap<>();
        out.put("imported", importedCount);
        out.put("skipped",  skippedCount);
        out.put("warnings", warningCount);
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(out)
           .send();
      }
      }
    }
  }

  private Path createTempFile(String filename, byte[] bytes) throws IOException
  {
    String ext;
    Path dir;
    Path file;

    ext  = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : ".xlsx";
    dir  = Path.of(tmpDir);
    Files.createDirectories(dir);
    file = Files.createTempFile(dir, "import_", ext);
    Files.write(file, bytes);
    return file;
  }

  private static HashMap<String, Object> applyMapping(Map<String, Object> row, HashMap<String, Object> mapping)
  {
    HashMap<String, Object> mapped;

    mapped = new HashMap<>();
    for (Map.Entry<String, Object> entry : mapping.entrySet()) {
      String colFile;
      String campoSys;
      colFile  = entry.getKey();
      campoSys = entry.getValue().toString();
      if (row.containsKey(colFile)) {
        mapped.put(campoSys, row.get(colFile));
      }
    }
    return mapped;
  }

  private static String stringify(Object val)
  {
    return val != null ? val.toString().trim() : "";
  }

  private static String normalize(String telefono)
  {
    return telefono.replaceAll("[\\s\\-]", "");
  }

  private static String capitalize(String s)
  {
    String result;
    String[] words;
    StringBuilder sb;

    if (s == null || s.isEmpty()) {
      result = s;
    } else {
      words = s.split("\\s+");
      sb    = new StringBuilder();
      for (String w : words) {
        if (!w.isEmpty()) {
          if (sb.length() > 0) {
            sb.append(' ');
          }
          sb.append(Character.toUpperCase(w.charAt(0)));
          if (w.length() > 1) {
            sb.append(w.substring(1).toLowerCase());
          }
        }
      }
      result = sb.toString();
    }
    return result;
  }

  private static HashMap<String, String> campo(String key, String label)
  {
    HashMap<String, String> m;

    m = new HashMap<>();
    m.put("key",   key);
    m.put("label", label);
    return m;
  }
}
