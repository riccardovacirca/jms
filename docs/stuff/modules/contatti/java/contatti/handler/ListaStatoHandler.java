package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ListaDAO;
import {{APP_PACKAGE}}.contatti.dto.ListaDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;

/** PUT /api/liste/{id}/stato — aggiorna solo lo stato della lista. Body: {"stato":1} */
public class ListaStatoHandler implements Handler
{
  private static final Log log = Log.get(ListaStatoHandler.class);

  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    int stato;
    ListaDAO dao;
    ListaDTO existing;
    HashMap<String, Object> body;

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

    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ListaDAO(db);
    existing = dao.findById(id);

    if (existing == null) {
      res.status(200).contentType("application/json").err(true).log("Lista non trovata").out(null).send();
      return;
    }

    body  = Json.decode(req.getBody(), HashMap.class);
    stato = ((Number) body.get("stato")).intValue();
    dao.updateStato(id, stato);
    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
