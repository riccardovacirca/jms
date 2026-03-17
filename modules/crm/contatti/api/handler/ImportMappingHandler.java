package dev.jms.app.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.contatti.dao.ImportSessionDAO;
import dev.jms.app.contatti.dto.ImportSessionDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;

/** PUT /api/import/{id}/mapping — salva la mappatura colonne → campi. */
public class ImportMappingHandler implements Handler
{
  private static final Log log = Log.get(ImportMappingHandler.class);

  /** Salva la mappatura colonne → campi per la sessione di importazione identificata dall'id nel path. */
  @SuppressWarnings("unchecked")
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String sessionId;
    ImportSessionDAO dao;
    ImportSessionDTO session;
    HashMap<String, Object> body;
    Object mappingObj;

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
        } else {
          body = Json.decode(req.getBody(), HashMap.class);
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
}
