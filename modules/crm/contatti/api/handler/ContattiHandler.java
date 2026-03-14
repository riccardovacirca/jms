package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.adapter.ContattoAdapter;
import {{APP_PACKAGE}}.contatti.dao.ContattoDAO;
import {{APP_PACKAGE}}.contatti.dto.ContattoDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

import java.util.HashMap;
import java.util.List;

/** GET /api/contatti — lista paginata con filtro opzionale per lista.
 *  POST /api/contatti — crea un nuovo contatto. */
public class ContattiHandler implements Handler
{
  private static final Log log = Log.get(ContattiHandler.class);

  /** Restituisce la lista paginata dei contatti, con filtro opzionale per lista (query param listaId). */
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String pageStr;
    String sizeStr;
    String listaIdStr;
    int page;
    int size;
    Integer listaId;
    ContattoDAO dao;
    List<ContattoDTO> items;
    int total;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("GET /api/contatti rifiutato: access_token assente");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        pageStr = req.getQueryParam("page");
        sizeStr = req.getQueryParam("size");
        listaIdStr = req.getQueryParam("listaId");
        page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        size = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
        listaId = listaIdStr != null ? Integer.parseInt(listaIdStr) : null;
        dao = new ContattoDAO(db);
        items = dao.findAll(page, size, listaId);
        total = dao.count(listaId);
        out = new HashMap<>();
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        out.put("items", items);
        out.put("hello", "world");
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(out)
           .send();
      } catch (JWTVerificationException e) {
        log.warn("GET /api/contatti rifiutato: token non valido o scaduto");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }

  /** Crea un nuovo contatto. Restituisce l'id generato. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    ContattoDTO contatto;
    ContattoDAO dao;
    int newId;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("POST /api/contatti rifiutato: access_token assente");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        try {
          dao = new ContattoDAO(db);
          contatto = ContattoAdapter.from(req);
          if (dao.existsByTelefono(contatto.telefono(), null)) {
            res.status(200)
               .contentType("application/json")
               .err(true)
               .log("Telefono già esistente")
               .out(null)
               .send();
          } else {
            newId = dao.insert(contatto);
            out = new HashMap<>();
            out.put("id", newId);
            res.status(200)
               .contentType("application/json")
               .err(false)
               .log(null)
               .out(out)
               .send();
          }
        } catch (ValidationException e) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log(e.getMessage())
             .out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("POST /api/contatti rifiutato: token non valido o scaduto");
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
