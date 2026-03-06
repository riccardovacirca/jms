package dev.crm.module.importer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crm.module.contatti.dao.ContattoDao;
import dev.crm.module.importer.dao.ImportSessionDao;
import dev.crm.module.importer.dao.ImporterDao;
import dev.crm.module.liste.dao.ListaDao;
import dev.crm.module.importer.dto.ImportResultDto;
import dev.crm.module.importer.dto.ImportSessionDto;
import dev.crm.module.liste.dto.ListaDto;
import dev.crm.module.importer.dto.ValidationIssueDto;
import dev.crm.module.importer.dto.ValidationResultDto;
import dev.springtools.util.excel.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ImporterService
{

  private final ImporterDao dao;
  private final ImportSessionDao sessionDao;
  private final ContattoDao contattoDao;
  private final ListaDao listaDao;
  private final ObjectMapper objectMapper;
  private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + File.separator + "crm-imports";

  public ImporterService(
      ImporterDao dao, ImportSessionDao sessionDao, ContattoDao contattoDao, ListaDao listaDao)
  {
    this.dao = dao;
    this.sessionDao = sessionDao;
    this.contattoDao = contattoDao;
    this.listaDao = listaDao;
    this.objectMapper = new ObjectMapper();

    // Crea la directory di upload se non esiste
    try {
      Files.createDirectories(Paths.get(UPLOAD_DIR));
    } catch (IOException e) {
      throw new RuntimeException("Impossibile creare directory upload", e);
    }
  }

  /** STEP 1: Analizza il file e crea una sessione di importazione */
  public ImportSessionDto analyzeFile(InputStream fileStream, String filename) throws Exception
  {
    // Genera ID sessione
    String sessionId = UUID.randomUUID().toString();

    // Salva il file temporaneamente
    String filePath = UPLOAD_DIR + File.separator + sessionId + "_" + filename;
    Path targetPath = Paths.get(filePath);
    Files.copy(fileStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

    // Analizza il file
    ExcelAnalyzer analyzer = new ExcelAnalyzer(new FileInputStream(filePath));
    ExcelAnalyzer.AnalysisResult analysis = analyzer.analyze(5); // Prime 5 righe di preview

    // Salva la sessione nel DB
    String headersJson = objectMapper.writeValueAsString(analysis.headers);
    sessionDao.insert(sessionId, filename, filePath, headersJson, analysis.totalRows);

    // Prepara la risposta
    ImportSessionDto session = new ImportSessionDto(
        sessionId, filename, analysis.headers, analysis.totalRows, analysis.previewRows);
    session.warnings = analysis.warnings;

    return session;
  }

  /** STEP 2: Salva il mapping delle colonne */
  public void saveColumnMapping(String sessionId, Map<String, String> columnMapping)
      throws Exception
  {
    String mappingJson = objectMapper.writeValueAsString(columnMapping);
    sessionDao.updateMapping(sessionId, mappingJson);
  }

  /** STEP 4: Esegue l'importazione finale dei dati validi */
  public ImportResultDto executeImport(String sessionId, Long listaId, String listaName)
      throws Exception
  {
    try {
      // Recupera la sessione
      ImportSessionDto session = sessionDao.findById(sessionId)
          .orElseThrow(() -> new Exception("Sessione non trovata"));

      // Verifica che la sessione sia stata validata
      if (!"validated".equals(session.status)) {
        throw new Exception("La sessione deve essere validata prima dell'importazione");
      }

      // Determina la lista a cui associare i contatti
      Long targetListaId = null;
      String targetListaName = null;

      if (listaId != null) {
        // Usa lista esistente
        targetListaId = listaId;
        ListaDto existingLista = listaDao.findById(listaId).orElseThrow(() -> new Exception("Lista non trovata"));
        targetListaName = existingLista.nome;
      } else if (listaName != null && !listaName.trim().isEmpty()) {
        // Verifica che non esista già una lista con questo nome
        if (listaDao.existsByNome(listaName.trim(), null)) {
          throw new Exception(
              "Esiste già una lista con il nome \""
                  + listaName.trim()
                  + "\". Scegli un nome diverso o seleziona la lista esistente.");
        }

        // Crea nuova lista
        ListaDto newLista = new ListaDto();
        newLista.nome = listaName.trim();
        newLista.descrizione = "Importata da " + session.filename;
        newLista.attiva = true;
        targetListaId = listaDao.insert(newLista);
        targetListaName = listaName.trim();
      }

      // Aggiorna status a "importing"
      sessionDao.updateStatus(sessionId, "importing");

      // Leggi il file
      String filePath = getFilePath(sessionId);
      ExcelReader reader = new ExcelReader(new FileInputStream(filePath));
      List<Map<String, Object>> rows = reader.read();

      // Recupera il mapping
      String mappingJson = getMappingFromSession(sessionId);
      Map<String, String> columnMapping = objectMapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {
      });

      // Contatori
      int imported = 0;
      int skipped = 0;

      // Set per tracciare telefoni già visti
      Set<String> seenPhones = new HashSet<>();

      // Lista ID contatti importati (per associazione lista)
      List<Long> contattiIds = new ArrayList<>();

      // Importa ogni riga valida
      for (Map<String, Object> row : rows) {
        // Applica mapping
        Map<String, Object> mappedRow = applyMapping(row, columnMapping);

        // Verifica validità (stessa logica della validazione)
        if (!isRowValid(mappedRow, seenPhones)) {
          skipped++;
          continue;
        }

        // Normalizza i dati
        normalizeRow(mappedRow);

        // Inserisci nel database
        try {
          dao.getDbRowConsumer().accept(mappedRow);
          imported++;

          // Recupera l'ID del contatto appena inserito
          String telefono = getString(mappedRow, "telefono");
          if (!isEmpty(telefono)) {
            seenPhones.add(telefono);
            // Trova l'ID del contatto tramite telefono
            Long contattoId = contattoDao.findIdByTelefono(telefono);
            if (contattoId != null) {
              contattiIds.add(contattoId);
            }
          }
        } catch (Exception e) {
          // Log errore ma continua con le altre righe
          System.err.println("Errore importazione riga: " + e.getMessage());
          skipped++;
        }
      }

      // Associa i contatti alla lista se specificata
      if (targetListaId != null) {
        for (Long contattoId : contattiIds) {
          try {
            listaDao.addContatto(targetListaId, contattoId);
          } catch (Exception e) {
            System.err.println(
                "Errore associazione contatto " + contattoId + " alla lista: " + e.getMessage());
          }
        }
      }

      // Aggiorna status a "completed"
      sessionDao.markCompleted(sessionId);

      ImportResultDto result = new ImportResultDto(imported);
      result.listaName = targetListaName;
      return result;

    } catch (Exception e) {
      // In caso di errore, marca la sessione come failed
      sessionDao.markFailed(sessionId, e.getMessage());
      throw e;
    }
  }

  /** Verifica se una riga è valida (da importare) */
  private boolean isRowValid(Map<String, Object> mappedRow, Set<String> seenPhones)
      throws Exception
  {
    String nome = getString(mappedRow, "nome");
    String cognome = getString(mappedRow, "cognome");
    String ragioneSociale = getString(mappedRow, "ragione_sociale");
    String telefono = getString(mappedRow, "telefono");

    // Deve avere almeno un identificatore
    if (isEmpty(nome) && isEmpty(cognome) && isEmpty(ragioneSociale)) {
      return false;
    }

    // Se ha telefono, verifica blacklist
    if (!isEmpty(telefono)) {
      if (contattoDao.isInBlacklist(telefono)) {
        return false;
      }

      // Skip duplicati nello stesso file
      if (seenPhones.contains(telefono)) {
        return false;
      }
    }

    return true;
  }

  /** Normalizza i dati prima dell'inserimento */
  private void normalizeRow(Map<String, Object> row)
  {
    // Normalizza telefono (rimuovi spazi, trattini, ecc.)
    String telefono = getString(row, "telefono");
    if (!isEmpty(telefono)) {
      telefono = telefono.replaceAll("[\\s\\-\\.]", "");
      row.put("telefono", telefono);
    }

    // Capitalizza nome e cognome
    String nome = getString(row, "nome");
    if (!isEmpty(nome)) {
      row.put("nome", capitalize(nome));
    }

    String cognome = getString(row, "cognome");
    if (!isEmpty(cognome)) {
      row.put("cognome", capitalize(cognome));
    }

    // Email lowercase
    String email = getString(row, "email");
    if (!isEmpty(email)) {
      row.put("email", email.toLowerCase().trim());
    }
  }

  private String capitalize(String str)
  {
    if (isEmpty(str))
      return str;
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  /** STEP 3: Valida i dati mappati */
  public ValidationResultDto validateData(String sessionId) throws Exception
  {
    // Recupera la sessione
    ImportSessionDto session = sessionDao.findById(sessionId).orElseThrow(() -> new Exception("Sessione non trovata"));

    // Leggi il file
    String filePath = getFilePath(sessionId);
    ExcelReader reader = new ExcelReader(new FileInputStream(filePath));
    List<Map<String, Object>> rows = reader.read();

    // Recupera il mapping
    String mappingJson = getMappingFromSession(sessionId);
    Map<String, String> columnMapping = objectMapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {
    });

    // Statistiche
    int totalRows = rows.size();
    int validRows = 0;
    int warningRows = 0;
    int errorRows = 0;
    List<ValidationIssueDto> issues = new ArrayList<>();

    // Set per tracciare telefoni già visti (per duplicati)
    Set<String> seenPhones = new HashSet<>();

    // Valida ogni riga
    int rowNumber = 1;
    for (Map<String, Object> row : rows) {
      rowNumber++;

      // Applica mapping
      Map<String, Object> mappedRow = applyMapping(row, columnMapping);

      boolean hasError = false;
      boolean hasWarning = false;

      // Validazione 1: Campi obbligatori (almeno un identificatore)
      String nome = getString(mappedRow, "nome");
      String cognome = getString(mappedRow, "cognome");
      String ragioneSociale = getString(mappedRow, "ragione_sociale");
      String telefono = getString(mappedRow, "telefono");

      if (isEmpty(nome) && isEmpty(cognome) && isEmpty(ragioneSociale)) {
        issues.add(
            new ValidationIssueDto(
                rowNumber,
                "error",
                "missing_field",
                "Manca un identificatore (Nome, Cognome o Ragione Sociale)",
                mappedRow));
        hasError = true;
      }

      // Validazione 2: Telefono mancante (warning)
      if (isEmpty(telefono)) {
        issues.add(
            new ValidationIssueDto(
                rowNumber, "warning", "missing_field", "Telefono mancante", mappedRow));
        hasWarning = true;
      }

      // Validazione 3: Duplicati (stesso telefono nel file)
      if (!isEmpty(telefono)) {
        if (seenPhones.contains(telefono)) {
          issues.add(
              new ValidationIssueDto(
                  rowNumber,
                  "warning",
                  "duplicate",
                  "Telefono duplicato nel file: " + telefono,
                  mappedRow));
          hasWarning = true;
        } else {
          seenPhones.add(telefono);

          // Validazione 4: Telefono già esistente nel database
          if (contattoDao.existsByTelefono(telefono)) {
            issues.add(
                new ValidationIssueDto(
                    rowNumber,
                    "warning",
                    "duplicate",
                    "Telefono già presente nel database: " + telefono,
                    mappedRow));
            hasWarning = true;
          }
        }

        // Validazione 5: Blacklist
        if (contattoDao.isInBlacklist(telefono)) {
          issues.add(
              new ValidationIssueDto(
                  rowNumber, "error", "blacklist", "Numero in blacklist: " + telefono, mappedRow));
          hasError = true;
        }
      }

      // Conteggio
      if (hasError) {
        errorRows++;
      } else if (hasWarning) {
        warningRows++;
      } else {
        validRows++;
      }
    }

    // Aggiorna status della sessione
    sessionDao.updateStatus(sessionId, "validated");

    return new ValidationResultDto(sessionId, totalRows, validRows, warningRows, errorRows, issues);
  }

  private Map<String, Object> applyMapping(
      Map<String, Object> row, Map<String, String> columnMapping)
  {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
      String headerName = entry.getKey();
      String fieldName = entry.getValue();
      if (fieldName != null && !fieldName.isEmpty()) {
        result.put(fieldName, row.get(headerName));
      }
    }
    return result;
  }

  private String getString(Map<String, Object> map, String key)
  {
    Object value = map.get(key);
    if (value == null)
      return null;
    return value.toString().trim();
  }

  private boolean isEmpty(String str)
  {
    return str == null || str.trim().isEmpty();
  }

  private String getFilePath(String sessionId) throws Exception
  {
    return sessionDao.getFilePath(sessionId);
  }

  private String getMappingFromSession(String sessionId) throws Exception
  {
    String mapping = sessionDao.getColumnMapping(sessionId);
    return mapping != null ? mapping : "{}";
  }

  /** Import da file Excel già presente (vecchio metodo, manteniamo per compatibilità) */
  public ImportResultDto importFromFile(String path, ImportConfig config) throws Exception
  {
    try (InputStream is = new FileInputStream(path)) {
      ExcelImporter importer = new ExcelImporter(is, config.getMappingStrategy(), config.getNormalizationStrategy());
      ImportResult result = importer.execute(dao.getDbRowConsumer());
      return new ImportResultDto(result.getRowsImported());
    }
  }

  /** Import da InputStream (vecchio metodo, da deprecare) */
  public ImportResultDto importFromStream(InputStream is, ImportConfig config) throws Exception
  {
    ExcelImporter importer = new ExcelImporter(is, config.getMappingStrategy(), config.getNormalizationStrategy());
    ImportResult result = importer.execute(dao.getDbRowConsumer());
    return new ImportResultDto(result.getRowsImported());
  }

  /** Ottiene lo storico delle importazioni */
  public List<ImportSessionDto> getImportHistory(int limit, int offset) throws Exception
  {
    return sessionDao.findAll(limit, offset);
  }

  /** Conta le sessioni di importazione */
  public int getImportHistoryCount() throws Exception
  {
    return sessionDao.count();
  }
}
