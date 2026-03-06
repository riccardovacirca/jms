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

import java.util.Map;

/** PUT /api/import/{id}/mapping — salva la mappatura colonne → campi. */
public class ImportMappingHandler implements Handler
{
  private static final Log log = Log.get(ImportMappingHandler.class);

  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String           token;
    String           sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO session;
    Map<?, ?>        body;
    Object           mappingObj;

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

    body       = Json.decode(req.getBody(), Map.class);
    mappingObj = body.get("mapping");
    if (mappingObj == null) {
      res.status(200).contentType("application/json").err(true).log("Parametro 'mapping' mancante").out(null).send();
      return;
    }

    dao.updateMapping(sessionId, Json.encode(mappingObj));

    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
