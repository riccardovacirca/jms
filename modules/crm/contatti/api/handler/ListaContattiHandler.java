package dev.jms.app.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.contatti.dao.ListaDAO;
import dev.jms.app.contatti.dto.ListaContattoDTO;
import dev.jms.app.contatti.dto.ListaDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;
import java.util.List;

/** GET  /api/liste/{id}/contatti — contatti della lista, paginati.
 *  POST /api/liste/{id}/contatti — aggiunge un contatto alla lista. Body: {"contattoId":42} */
public class ListaContattiHandler implements Handler
{
  private static final Log log = Log.get(ListaContattiHandler.class);

  /** Restituisce i contatti della lista paginati. */
  @SuppressWarnings("unchecked")
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    ListaDAO dao;
    ListaDTO lista;
    List<ListaContattoDTO> items;
    int total;
    HashMap<String, Object> out;

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
        lista = dao.findById(id);
        if (lista == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Lista non trovata")
             .out(null)
             .send();
        } else {
          pageStr = req.getQueryParam("page");
          sizeStr = req.getQueryParam("size");
          page = pageStr != null ? Integer.parseInt(pageStr) : 1;
          size = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
          items = dao.findContatti(id, page, size);
          total = dao.countContatti(id);
          out = new HashMap<>();
          out.put("total", total);
          out.put("page", page);
          out.put("size", size);
          out.put("items", items);
          res.status(200)
             .contentType("application/json")
             .err(false)
             .log(null)
             .out(out)
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

  /** Aggiunge un contatto alla lista. */
  @SuppressWarnings("unchecked")
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    int contattoId;
    ListaDAO dao;
    ListaDTO lista;
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
        lista = dao.findById(id);
        if (lista == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Lista non trovata")
             .out(null)
             .send();
        } else {
          body = Json.decode(req.getBody(), HashMap.class);
          contattoId = ((Number) body.get("contattoId")).intValue();
          dao.addContatto(id, contattoId);
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
