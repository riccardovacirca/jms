package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ContattoDAO;
import {{APP_PACKAGE}}.contatti.dao.ImportSessionDAO;
import {{APP_PACKAGE}}.contatti.dao.ListaDAO;
import {{APP_PACKAGE}}.contatti.dto.ContattoDTO;
import {{APP_PACKAGE}}.contatti.dto.ImportSessionDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.excel.ExcelReader;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** POST /api/import/{id}/execute — esegue l'importazione dei contatti. */
public class ImportExecuteHandler implements Handler
{
  private static final Log log = Log.get(ImportExecuteHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String           token;
    String           sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO session;
    Map<?, ?>        body;
    Map<?, ?>        mapping;
    Integer          listaId;
    boolean          consenso;
    List<Map<String, Object>> rows;
    ContattoDAO      contattoDao;
    ListaDAO         listaDao;
    int              importedCount;
    int              skippedCount;
    int              warningCount;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
      return;
    }
    try {
      Auth.get().verifyAccessToken(token);
    } catch (JWTVerificationException e) {
      res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      return;
    }

    sessionId = req.urlArgs().get("id");
    dao       = new ImportSessionDAO(db);
    session   = dao.findById(sessionId);

    if (session == null) {
      res.status(200).contentType("application/json").err(true).log("Sessione non trovata").out(null).send();
      return;
    }
    if (session.columnMapping() == null) {
      res.status(200).contentType("application/json").err(true).log("Mappatura colonne non configurata").out(null).send();
      return;
    }
    if (!Files.exists(Paths.get(session.filePath()))) {
      res.status(200).contentType("application/json").err(true).log("File di importazione non trovato").out(null).send();
      return;
    }

    body    = Json.decode(req.getBody(), Map.class);
    listaId = body.get("listaId") != null ? ((Number) body.get("listaId")).intValue() : null;
    consenso = body.get("consenso") instanceof Boolean ? (Boolean) body.get("consenso") : false;

    mapping     = Json.decode(session.columnMapping(), Map.class);
    rows        = new ExcelReader(new FileInputStream(session.filePath())).read();

    contattoDao  = new ContattoDAO(db);
    listaDao     = listaId != null ? new ListaDAO(db) : null;
    importedCount = 0;
    skippedCount  = 0;
    warningCount  = 0;

    db.begin();
    try {
      for (Map<String, Object> row : rows) {
        Map<String, Object> mapped = ImportValidateHandler.applyMapping(row, mapping);

        String nome           = capitalize(ImportValidateHandler.stringify(mapped.get("nome")));
        String cognome        = capitalize(ImportValidateHandler.stringify(mapped.get("cognome")));
        String ragioneSociale = ImportValidateHandler.stringify(mapped.get("ragione_sociale"));
        String telefono       = ImportValidateHandler.normalize(ImportValidateHandler.stringify(mapped.get("telefono")));
        String email          = ImportValidateHandler.stringify(mapped.get("email")).toLowerCase();
        String indirizzo      = ImportValidateHandler.stringify(mapped.get("indirizzo"));
        String citta          = ImportValidateHandler.stringify(mapped.get("citta"));
        String cap            = ImportValidateHandler.stringify(mapped.get("cap"));
        String provincia      = ImportValidateHandler.stringify(mapped.get("provincia"));
        String note           = ImportValidateHandler.stringify(mapped.get("note"));

        // Salta righe senza identificatore
        if (nome.isEmpty() && cognome.isEmpty() && ragioneSociale.isEmpty()) {
          skippedCount++;
          continue;
        }

        // Salta contatti già presenti con stesso telefono
        if (!telefono.isEmpty() && contattoDao.existsByTelefono(telefono, null)) {
          warningCount++;
          continue;
        }

        ContattoDTO c = new ContattoDTO(
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

        int newId = contattoDao.insert(c);

        if (listaDao != null) {
          listaDao.addContatto(listaId, newId);
        }

        importedCount++;
      }
      db.commit();
    } catch (Exception e) {
      db.rollback();
      dao.updateStatus(sessionId, "failed", e.getMessage());
      log.error("Errore durante importazione sessione " + sessionId + ": " + e.getMessage());
      res.status(200).contentType("application/json").err(true).log("Errore durante l'importazione").out(null).send();
      return;
    }

    dao.markCompleted(sessionId);

    out = new HashMap<>();
    out.put("imported", importedCount);
    out.put("skipped",  skippedCount);
    out.put("warnings", warningCount);

    res.status(200).contentType("application/json").err(false).log(null).out(out).send();
  }

  private static String capitalize(String s)
  {
    if (s == null || s.isEmpty()) return s;
    String[] words = s.split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      if (!w.isEmpty()) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(Character.toUpperCase(w.charAt(0)));
        if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
      }
    }
    return sb.toString();
  }
}
