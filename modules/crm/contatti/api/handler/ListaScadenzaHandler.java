package dev.jms.app.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.contatti.dao.ListaDAO;
import dev.jms.app.contatti.dto.ListaDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;

/** PUT /api/liste/{id}/scadenza — aggiorna solo la scadenza della lista. Body: {"scadenza":"2026-12-31"} */
public class ListaScadenzaHandler implements Handler
{
  private static final Log log = Log.get(ListaScadenzaHandler.class);

  /** Aggiorna il campo scadenza della lista identificata dall'id nel path. */
  @SuppressWarnings("unchecked")
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    String scadenza;
    ListaDAO dao;
    ListaDTO existing;
    HashMap<String, Object> body;

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
        id = Integer.parseInt(req.urlArgs().get("id"));
        dao = new ListaDAO(db);
        existing = dao.findById(id);
        if (existing == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Lista non trovata")
             .out(null)
             .send();
        } else {
          body = Json.decode(req.getBody(), HashMap.class);
          scadenza = (String) body.get("scadenza");
          dao.updateScadenza(id, scadenza);
          res.status(200)
             .contentType("application/json")
             .err(false)
             .log(null)
             .out(null)
             .send();
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
