package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ContattoDAO;
import {{APP_PACKAGE}}.contatti.dto.ContattoDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.HashMap;
import java.util.List;

/** GET /api/contatti/search?q=&page=&size= — ricerca full-text paginata. */
public class ContattiSearchHandler implements Handler
{
  private static final Log log = Log.get(ContattiSearchHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String query;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    ContattoDAO dao;
    List<ContattoDTO> items;
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

    query = req.getQueryParam("q");
    if (query == null || query.isBlank()) {
      res.status(200).contentType("application/json").err(true).log("Parametro q obbligatorio").out(null).send();
      return;
    }

    pageStr = req.getQueryParam("page");
    sizeStr = req.getQueryParam("size");
    page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
    size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;

    dao   = new ContattoDAO(db);
    items = dao.search(query, page, size);
    total = dao.countSearch(query);

    out = new HashMap<>();
    out.put("total", total);
    out.put("page",  page);
    out.put("size",  size);
    out.put("items", items);

    res.status(200).contentType("application/json").err(false).log(null).out(out).send();
  }
}
