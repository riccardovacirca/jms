package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ContattoDAO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;

/** PUT /api/contatti/{id}/stato — aggiorna solo il campo stato. Body: { "stato": 1 } */
public class ContattoStatoHandler implements Handler
{
  private static final Log log = Log.get(ContattoStatoHandler.class);

  @SuppressWarnings("unchecked")
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    HashMap<String, Object> body;
    int stato;
    ContattoDAO dao;

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

    id    = Integer.parseInt(req.urlArgs().get("id"));
    body  = Json.decode(req.getBody(), HashMap.class);
    stato = DB.toInteger(body.get("stato"));
    dao   = new ContattoDAO(db);
    dao.updateStato(id, stato);

    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
