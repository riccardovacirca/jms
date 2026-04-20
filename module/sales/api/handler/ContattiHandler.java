package dev.jms.app.sales.handler;

import dev.jms.app.sales.adapter.ContattoAdapter;
import dev.jms.app.sales.dao.ContattoDAO;
import dev.jms.app.sales.dto.ContattoDTO;
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

/** Handler per le operazioni sui contatti (tabella jms_sales_contatti). */
public class ContattiHandler
{
  private static final Log log = Log.get(ContattiHandler.class);

  /**
   * GET /api/sales/contatti — lista paginata con filtro opzionale per lista (query param {@code listaId}).
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
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

    session.require(Role.USER, Permission.READ);
    pageStr    = req.getQueryParam("page");
    sizeStr    = req.getQueryParam("size");
    listaIdStr = req.getQueryParam("listaId");
    page    = pageStr    != null ? Integer.parseInt(pageStr)    : 1;
    size    = sizeStr    != null ? Integer.parseInt(sizeStr)    : 20;
    listaId = listaIdStr != null ? Integer.parseInt(listaIdStr) : null;
    dao     = new ContattoDAO(db);
    items   = dao.findAll(page, size, listaId);
    total   = dao.count(listaId);
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
   * GET /api/sales/contatti/search — ricerca full-text paginata (query param {@code q}).
   */
  public void search(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String query;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    ContattoDAO dao;
    List<ContattoDTO> items;
    int total;
    HashMap<String, Object> out;

    session.require(Role.USER, Permission.READ);
    query = req.getQueryParam("q");
    if (query == null || query.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro q obbligatorio")
         .out(null)
         .send();
    } else {
      pageStr = req.getQueryParam("page");
      sizeStr = req.getQueryParam("size");
      page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
      size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
      dao     = new ContattoDAO(db);
      items   = dao.search(query, page, size);
      total   = dao.countSearch(query);
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
   * POST /api/sales/contatti — crea un nuovo contatto. Restituisce l'id generato.
   */
  public void create(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    ContattoDAO dao;
    ContattoDTO contatto;
    int newId;
    HashMap<String, Object> out;

    session.require(Role.USER, Permission.WRITE);
    try {
      dao      = new ContattoDAO(db);
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
   * GET /api/sales/contatti/{id} — recupera un contatto per id.
   */
  public void get(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ContattoDAO dao;
    ContattoDTO contatto;

    session.require(Role.USER, Permission.READ);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ContattoDAO(db);
    contatto = dao.findById(id);
    if (contatto == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Contatto non trovato")
         .out(null)
         .send();
    } else {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(contatto)
         .send();
    }
  }

  /**
   * PUT /api/sales/contatti/{id} — aggiorna tutti i campi del contatto.
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ContattoDAO dao;
    ContattoDTO existing;
    ContattoDTO updated;

    session.require(Role.USER, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ContattoDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Contatto non trovato")
         .out(null)
         .send();
    } else {
      try {
        updated = ContattoAdapter.from(req);
        if (dao.existsByTelefono(updated.telefono(), id)) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Telefono già esistente")
             .out(null)
             .send();
        } else {
          updated = new ContattoDTO(
            id,
            updated.nome(), updated.cognome(), updated.ragioneSociale(),
            updated.telefono(), updated.email(), updated.indirizzo(),
            updated.citta(), updated.cap(), updated.provincia(), updated.note(),
            updated.stato(), updated.consenso(), updated.blacklist(),
            existing.createdAt(), null, existing.listeCount()
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
   * DELETE /api/sales/contatti/{id} — elimina un contatto.
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    ContattoDAO dao;
    ContattoDTO existing;

    session.require(Role.USER, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new ContattoDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Contatto non trovato")
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
   * PUT /api/sales/contatti/{id}/stato — aggiorna solo il campo stato. Body: {@code {"stato": 1}}.
   */
  @SuppressWarnings("unchecked")
  public void updateStato(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    HashMap<String, Object> body;
    int stato;
    ContattoDAO dao;

    session.require(Role.USER, Permission.WRITE);
    id    = Integer.parseInt(req.urlArgs().get("id"));
    body  = Json.decode(req.getBody(), HashMap.class);
    stato = DB.toInteger(body.get("stato"));
    dao   = new ContattoDAO(db);
    dao.updateStato(id, stato);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * PUT /api/sales/contatti/{id}/blacklist — aggiorna solo il flag blacklist. Body: {@code {"blacklist": true}}.
   */
  @SuppressWarnings("unchecked")
  public void updateBlacklist(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    HashMap<String, Object> body;
    boolean blacklist;
    ContattoDAO dao;

    session.require(Role.USER, Permission.WRITE);
    id        = Integer.parseInt(req.urlArgs().get("id"));
    body      = Json.decode(req.getBody(), HashMap.class);
    blacklist = DB.toBoolean(body.get("blacklist"));
    dao       = new ContattoDAO(db);
    dao.setBlacklist(id, blacklist);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }
}
