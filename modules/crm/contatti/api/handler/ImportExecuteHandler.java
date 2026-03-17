package dev.jms.app.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.contatti.dao.ContattoDAO;
import dev.jms.app.contatti.dao.ImportSessionDAO;
import dev.jms.app.contatti.dao.ListaDAO;
import dev.jms.app.contatti.dto.ContattoDTO;
import dev.jms.app.contatti.dto.ImportSessionDTO;
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

  /** Legge il file Excel, applica la mappatura e inserisce i contatti nel database. */
  @SuppressWarnings("unchecked")
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO session;
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

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        sessionId = req.urlArgs().get("id");
        dao = new ImportSessionDAO(db);
        session = dao.findById(sessionId);
        if (session == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Sessione non trovata")
             .out(null)
             .send();
        } else if (session.columnMapping() == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Mappatura colonne non configurata")
             .out(null)
             .send();
        } else if (!Files.exists(Paths.get(session.filePath()))) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("File di importazione non trovato")
             .out(null)
             .send();
        } else {
          body = Json.decode(req.getBody(), HashMap.class);
          listaId = body.get("listaId") != null ? ((Number) body.get("listaId")).intValue() : null;
          consenso = body.get("consenso") instanceof Boolean ? (Boolean) body.get("consenso") : false;
          mapping = Json.decode(session.columnMapping(), HashMap.class);
          rows = new ExcelReader(new FileInputStream(session.filePath())).read();
          contattoDao = new ContattoDAO(db);
          listaDao = listaId != null ? new ListaDAO(db) : null;
          importedCount = 0;
          skippedCount = 0;
          warningCount = 0;
          txFailed = false;
          txError = null;
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
              mapped = ImportValidateHandler.applyMapping(row, mapping);
              nome = capitalize(ImportValidateHandler.stringify(mapped.get("nome")));
              cognome = capitalize(ImportValidateHandler.stringify(mapped.get("cognome")));
              ragioneSociale = ImportValidateHandler.stringify(mapped.get("ragione_sociale"));
              telefono = ImportValidateHandler.normalize(ImportValidateHandler.stringify(mapped.get("telefono")));
              email = ImportValidateHandler.stringify(mapped.get("email")).toLowerCase();
              indirizzo = ImportValidateHandler.stringify(mapped.get("indirizzo"));
              citta = ImportValidateHandler.stringify(mapped.get("citta"));
              cap = ImportValidateHandler.stringify(mapped.get("cap"));
              provincia = ImportValidateHandler.stringify(mapped.get("provincia"));
              note = ImportValidateHandler.stringify(mapped.get("note"));
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
                nome.isEmpty() ? null : nome,
                cognome.isEmpty() ? null : cognome,
                ragioneSociale.isEmpty() ? null : ragioneSociale,
                telefono.isEmpty() ? null : telefono,
                email.isEmpty() ? null : email,
                indirizzo.isEmpty() ? null : indirizzo,
                citta.isEmpty() ? null : citta,
                cap.isEmpty() ? null : cap,
                provincia.isEmpty() ? null : provincia,
                note.isEmpty() ? null : note,
                1,
                consenso,
                false,
                null,
                null,
                0L
              );
              newId = contattoDao.insert(c);
              if (listaDao != null) {
                listaDao.addContatto(listaId, newId);
              }
              importedCount++;
            }
            db.commit();
          } catch (Exception e) {
            db.rollback();
            txFailed = true;
            txError = e.getMessage();
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
            out.put("skipped", skippedCount);
            out.put("warnings", warningCount);
            res.status(200)
               .contentType("application/json")
               .err(false)
               .log(null)
               .out(out)
               .send();
          }
        }
      } catch (JWTVerificationException e) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }

  /** Capitalizza ogni parola della stringa (prima lettera maiuscola, resto minuscolo). */
  private static String capitalize(String s)
  {
    String result;
    String[] words;
    StringBuilder sb;
    if (s == null || s.isEmpty()) {
      result = s;
    } else {
      words = s.split("\\s+");
      sb = new StringBuilder();
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
}
