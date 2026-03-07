package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ImportSessionDAO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** GET /api/import/{id}/validate — valida le righe del file con la mappatura salvata. */
public class ImportValidateHandler implements Handler
{
  private static final Log log = Log.get(ImportValidateHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String           token;
    String           sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO session;
    Map<?, ?>        mapping;
    List<Map<String, Object>> rows;
    List<Map<String, Object>> result;
    Set<String>      seenPhones;
    int              validCount;
    int              errorCount;
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

    mapping = Json.decode(session.columnMapping(), Map.class);
    rows    = new ExcelReader(new FileInputStream(session.filePath())).read();

    result       = new ArrayList<>();
    seenPhones   = new HashSet<>();
    validCount   = 0;
    errorCount   = 0;
    warningCount = 0;

    for (Map<String, Object> row : rows) {
      Map<String, Object> mapped   = applyMapping(row, mapping);
      List<String>        errors   = new ArrayList<>();
      List<String>        warnings = new ArrayList<>();

      String nome           = stringify(mapped.get("nome"));
      String cognome        = stringify(mapped.get("cognome"));
      String ragioneSociale = stringify(mapped.get("ragione_sociale"));
      String telefono       = normalize(stringify(mapped.get("telefono")));

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

      HashMap<String, Object> r = new HashMap<>();
      r.put("data",     mapped);
      r.put("errors",   errors);
      r.put("warnings", warnings);
      r.put("status",   errors.isEmpty() ? (warnings.isEmpty() ? "ok" : "warning") : "error");
      result.add(r);

      if (!errors.isEmpty()) errorCount++;
      else if (!warnings.isEmpty()) warningCount++;
      else validCount++;
    }

    out = new HashMap<>();
    out.put("valid",    validCount);
    out.put("errors",   errorCount);
    out.put("warnings", warningCount);
    out.put("rows",     result);

    res.status(200).contentType("application/json").err(false).log(null).out(out).send();
  }

  static Map<String, Object> applyMapping(Map<String, Object> row, Map<?, ?> mapping)
  {
    Map<String, Object> mapped = new HashMap<>();
    for (Map.Entry<?, ?> entry : mapping.entrySet()) {
      String colFile  = entry.getKey().toString();
      String campoSys = entry.getValue().toString();
      if (row.containsKey(colFile)) {
        mapped.put(campoSys, row.get(colFile));
      }
    }
    return mapped;
  }

  static String stringify(Object val)
  {
    return val != null ? val.toString().trim() : "";
  }

  static String normalize(String telefono)
  {
    return telefono.replaceAll("[\\s\\-]", "");
  }
}
