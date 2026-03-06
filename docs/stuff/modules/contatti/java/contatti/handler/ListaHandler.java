package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.adapter.ListaAdapter;
import {{APP_PACKAGE}}.contatti.dao.ListaDAO;
import {{APP_PACKAGE}}.contatti.dto.ListaDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

/** GET /api/liste/{id}    — recupera una lista.
 *  PUT /api/liste/{id}    — aggiorna una lista.
 *  DELETE /api/liste/{id} — elimina una lista (soft delete). */
public class ListaHandler implements Handler
{
  private static final Log log = Log.get(ListaHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ListaDAO dao;
    ListaDTO lista;

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
    dao   = new ListaDAO(db);
    lista = dao.findById(id);

    if (lista == null) {
      res.status(200).contentType("application/json").err(true).log("Lista non trovata").out(null).send();
    } else {
      res.status(200).contentType("application/json").err(false).log(null).out(lista).send();
    }
  }

  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ListaDAO dao;
    ListaDTO existing;
    ListaDTO updated;

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

    try {
      updated = ListaAdapter.from(req);
      if (dao.existsByNome(updated.nome(), id)) {
        res.status(200).contentType("application/json").err(true).log("Nome già esistente").out(null).send();
        return;
      }
      updated = new ListaDTO(
        id,
        updated.nome(), updated.descrizione(), updated.consenso(),
        updated.stato(), updated.scadenza(),
        existing.createdAt(), null, null, existing.contattiCount()
      );
      dao.update(updated);
      res.status(200).contentType("application/json").err(false).log(null).out(null).send();
    } catch (ValidationException e) {
      res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
    }
  }

  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ListaDAO dao;
    ListaDTO existing;

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

    dao.delete(id);
    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
