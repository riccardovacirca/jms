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

import java.util.HashMap;
import java.util.List;

/** GET /api/liste  — lista paginata.
 *  POST /api/liste — crea una nuova lista. */
public class ListeHandler implements Handler
{
  private static final Log log = Log.get(ListeHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    ListaDAO dao;
    List<ListaDTO> items;
    int total;
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

    pageStr = req.getQueryParam("page");
    sizeStr = req.getQueryParam("size");
    page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
    size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;

    dao   = new ListaDAO(db);
    items = dao.findAll(page, size);
    total = dao.count();

    out = new HashMap<>();
    out.put("total", total);
    out.put("page",  page);
    out.put("size",  size);
    out.put("items", items);

    res.status(200).contentType("application/json").err(false).log(null).out(out).send();
  }

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    ListaDTO lista;
    ListaDAO dao;
    int newId;
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

    try {
      dao   = new ListaDAO(db);
      lista = ListaAdapter.from(req);
      if (dao.existsByNome(lista.nome(), null)) {
        res.status(200).contentType("application/json").err(true).log("Nome già esistente").out(null).send();
        return;
      }
      newId = dao.insert(lista);
      out   = new HashMap<>();
      out.put("id", newId);
      res.status(200).contentType("application/json").err(false).log(null).out(out).send();
    } catch (ValidationException e) {
      res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
    }
  }
}
