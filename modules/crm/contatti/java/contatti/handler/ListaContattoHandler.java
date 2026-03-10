package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ListaDAO;
import {{APP_PACKAGE}}.contatti.dto.ListaDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

/** DELETE /api/liste/{id}/contatti/{cid} — rimuove un contatto dalla lista. */
public class ListaContattoHandler implements Handler
{
  private static final Log log = Log.get(ListaContattoHandler.class);

  /** Rimuove il contatto identificato da {cid} dalla lista identificata da {id}. */
  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    int cid;
    ListaDAO dao;
    ListaDTO lista;

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
        cid = Integer.parseInt(req.urlArgs().get("cid"));
        dao = new ListaDAO(db);
        lista = dao.findById(id);
        if (lista == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Lista non trovata")
             .out(null)
             .send();
        } else {
          dao.removeContatto(id, cid);
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
