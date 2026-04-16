package dev.jms.app.crm.handler;

import dev.jms.app.crm.adapter.ListaAdapter;
import dev.jms.app.crm.dao.ListaDAO;
import dev.jms.app.crm.dto.ListaContattoDTO;
import dev.jms.app.crm.dto.ListaDTO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.ValidationException;

import java.util.HashMap;
import java.util.List;

/**
 * Handler per le operazioni sulle liste di contatti (tabella liste).
 */
public class ListeHandler
{
  private static final Log log = Log.get(ListeHandler.class);

  /**
   * GET /api/liste — lista paginata delle liste.
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String pageStr;
    String sizeStr;
    int page;
    int size;
    ListaDAO dao;
    List<ListaDTO> items;
    int total;
    HashMap<String, Object> out;

    session.require(Role.USER, Permission.READ);
    pageStr = req.getQueryParam("page");
    sizeStr = req.getQueryParam("size");
    page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
    size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
    dao     = new ListaDAO(db);
    items   = dao.findAll(page, size);
    total   = dao.count();
    out     = new HashMap<>();
    out.put("total", total);
    out.put("page",  page);
    out.put("size",  size);
    out.put("items", items);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * POST /api/liste — crea una nuova lista. Restituisce l'id generato.
   */
  public void create(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    ListaDAO dao;
    ListaDTO lista;
    int newId;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    try {
      dao   = new ListaDAO(db);
      lista = ListaAdapter.from(req);
      if (dao.existsByNome(lista.nome(), null)) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Nome già esistente")
           .out(null)
           .send();
      } else {
        newId = dao.insert(lista);
        out   = new HashMap<>();
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
  }

  /**
   * GET /api/liste/{id} — recupera una lista per id.
   */
  public void get(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO lista;

    session.require(Role.USER, Permission.READ);
    id    = Integer.parseInt(req.urlArgs().get("id"));
    dao   = new ListaDAO(db);
    lista = dao.findById(id);
    if (lista == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(lista)
         .send();
    }
  }

  /**
   * PUT /api/liste/{id} — aggiorna tutti i campi della lista.
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO existing;
    ListaDTO updated;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ListaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      try {
        updated = ListaAdapter.from(req);
        if (dao.existsByNome(updated.nome(), id)) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Nome già esistente")
             .out(null)
             .send();
        } else {
          updated = new ListaDTO(
            id,
            updated.nome(), updated.descrizione(), updated.consenso(),
            updated.stato(), updated.scadenza(),
            existing.createdAt(), null, null, existing.isDefault(), existing.contattiCount()
          );
          dao.update(updated);
          res.status(200)
             .contentType("application/json")
             .err(false)
             .log(null)
             .out(null)
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
    }
  }

  /**
   * DELETE /api/liste/{id} — elimina una lista (soft delete).
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO existing;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ListaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else if (existing.isDefault()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("La lista di default non può essere eliminata")
         .out(null)
         .send();
    } else {
      dao.delete(id);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * PUT /api/liste/{id}/stato — aggiorna solo il campo stato. Body: {@code {"stato": 1}}.
   */
  @SuppressWarnings("unchecked")
  public void updateStato(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO existing;
    HashMap<String, Object> body;
    int stato;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ListaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      body  = Json.decode(req.getBody(), HashMap.class);
      stato = ((Number) body.get("stato")).intValue();
      dao.updateStato(id, stato);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * PUT /api/liste/{id}/scadenza — aggiorna solo la scadenza. Body: {@code {"scadenza": "2026-12-31"}}.
   */
  @SuppressWarnings("unchecked")
  public void updateScadenza(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO existing;
    HashMap<String, Object> body;
    String scadenza;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ListaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      body     = Json.decode(req.getBody(), HashMap.class);
      scadenza = (String) body.get("scadenza");
      dao.updateScadenza(id, scadenza);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * GET /api/liste/{id}/contatti — contatti della lista, paginati.
   */
  @SuppressWarnings("unchecked")
  public void listContatti(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
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

    session.require(Role.USER, Permission.READ);
    id      = Integer.parseInt(req.urlArgs().get("id"));
    dao     = new ListaDAO(db);
    lista   = dao.findById(id);
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
      page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
      size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
      items   = dao.findContatti(id, page, size);
      total   = dao.countContatti(id);
      out     = new HashMap<>();
      out.put("total", total);
      out.put("page",  page);
      out.put("size",  size);
      out.put("items", items);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    }
  }

  /**
   * POST /api/liste/{id}/contatti — aggiunge un contatto alla lista. Body: {@code {"contattoId": 42}}.
   */
  @SuppressWarnings("unchecked")
  public void addContatto(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO lista;
    HashMap<String, Object> body;
    int contattoId;

    session.require(Role.USER, Permission.WRITE);
    id    = Integer.parseInt(req.urlArgs().get("id"));
    dao   = new ListaDAO(db);
    lista = dao.findById(id);
    if (lista == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      body       = Json.decode(req.getBody(), HashMap.class);
      contattoId = ((Number) body.get("contattoId")).intValue();
      dao.addContatto(id, contattoId);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * DELETE /api/liste/{id}/contatti/{cid} — rimuove un contatto dalla lista.
   */
  public void removeContatto(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    int cid;
    ListaDAO dao;
    ListaDTO lista;

    session.require(Role.USER, Permission.WRITE);
    id    = Integer.parseInt(req.urlArgs().get("id"));
    cid   = Integer.parseInt(req.urlArgs().get("cid"));
    dao   = new ListaDAO(db);
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
  }

  /**
   * GET /api/crm/liste/default — restituisce la lista marcata come default, o null.
   */
  public void getDefault(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    ListaDAO dao;
    ListaDTO lista;

    session.require(Role.USER, Permission.READ);
    dao   = new ListaDAO(db);
    lista = dao.findDefault();
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(lista)
       .send();
  }

  /**
   * PUT /api/crm/liste/{id}/default — imposta la lista indicata come default.
   */
  public void setDefault(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ListaDAO dao;
    ListaDTO lista;

    session.require(Role.ADMIN, Permission.WRITE);
    id    = Integer.parseInt(req.urlArgs().get("id"));
    dao   = new ListaDAO(db);
    lista = dao.findById(id);
    if (lista == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Lista non trovata")
         .out(null)
         .send();
    } else {
      dao.setDefault(id);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }
}
