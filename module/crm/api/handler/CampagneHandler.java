package dev.jms.app.crm.handler;

import dev.jms.app.crm.adapter.CampagnaAdapter;
import dev.jms.app.crm.dao.CampagnaDAO;
import dev.jms.app.crm.dto.CampagnaDTO;
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
 * Handler per le operazioni sulle campagne CRM (tabella jms_crm_campagne).
 * Una campagna raggruppa una o più liste di contatti.
 */
public class CampagneHandler
{
  private static final Log log = Log.get(CampagneHandler.class);

  /**
   * GET /api/campagne — lista paginata delle campagne.
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String pageStr;
    String sizeStr;
    int page;
    int size;
    CampagnaDAO dao;
    List<CampagnaDTO> items;
    int total;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.READ);
    pageStr = req.getQueryParam("page");
    sizeStr = req.getQueryParam("size");
    page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
    size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
    dao     = new CampagnaDAO(db);
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
   * POST /api/campagne — crea una nuova campagna. Restituisce l'id generato.
   */
  public void create(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    CampagnaDAO dao;
    CampagnaDTO campagna;
    int newId;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    try {
      dao      = new CampagnaDAO(db);
      campagna = CampagnaAdapter.from(req);
      if (dao.existsByNome(campagna.nome(), null)) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Nome già esistente")
           .out(null)
           .send();
      } else {
        newId = dao.insert(campagna);
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
   * GET /api/campagne/{id} — recupera una campagna per id.
   */
  public void get(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    CampagnaDAO dao;
    CampagnaDTO campagna;

    session.require(Role.ADMIN, Permission.READ);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new CampagnaDAO(db);
    campagna = dao.findById(id);
    if (campagna == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
         .out(null)
         .send();
    } else {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(campagna)
         .send();
    }
  }

  /**
   * PUT /api/campagne/{id} — aggiorna tutti i campi della campagna.
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    CampagnaDAO dao;
    CampagnaDTO existing;
    CampagnaDTO updated;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new CampagnaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
         .out(null)
         .send();
    } else {
      try {
        updated = CampagnaAdapter.from(req);
        if (dao.existsByNome(updated.nome(), id)) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Nome già esistente")
             .out(null)
             .send();
        } else {
          updated = new CampagnaDTO(
            id,
            updated.nome(), updated.descrizione(), updated.stato(),
            existing.createdAt(), null, null, existing.listeCount()
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
   * DELETE /api/campagne/{id} — elimina una campagna (soft delete).
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    CampagnaDAO dao;
    CampagnaDTO existing;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new CampagnaDAO(db);
    existing = dao.findById(id);
    if (existing == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
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
   * GET /api/campagne/{id}/liste — liste associate alla campagna, paginate.
   */
  public void listListe(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    CampagnaDAO dao;
    CampagnaDTO campagna;
    List<ListaDTO> items;
    int total;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.READ);
    id      = Integer.parseInt(req.urlArgs().get("id"));
    dao     = new CampagnaDAO(db);
    campagna = dao.findById(id);
    if (campagna == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
         .out(null)
         .send();
    } else {
      pageStr = req.getQueryParam("page");
      sizeStr = req.getQueryParam("size");
      page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
      size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
      items   = dao.findListe(id, page, size);
      total   = dao.countListe(id);
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
   * POST /api/campagne/{id}/liste — aggiunge una lista alla campagna. Body: {@code {"listaId": 42}}.
   */
  @SuppressWarnings("unchecked")
  public void addLista(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    CampagnaDAO dao;
    CampagnaDTO campagna;
    HashMap<String, Object> body;
    int listaId;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    dao      = new CampagnaDAO(db);
    campagna = dao.findById(id);
    if (campagna == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
         .out(null)
         .send();
    } else {
      body    = Json.decode(req.getBody(), HashMap.class);
      listaId = ((Number) body.get("listaId")).intValue();
      dao.addLista(id, listaId);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * DELETE /api/campagne/{id}/liste/{lid} — rimuove una lista dalla campagna.
   */
  public void removeLista(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int id;
    int lid;
    CampagnaDAO dao;
    CampagnaDTO campagna;

    session.require(Role.ADMIN, Permission.WRITE);
    id       = Integer.parseInt(req.urlArgs().get("id"));
    lid      = Integer.parseInt(req.urlArgs().get("lid"));
    dao      = new CampagnaDAO(db);
    campagna = dao.findById(id);
    if (campagna == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Campagna non trovata")
         .out(null)
         .send();
    } else {
      dao.removeLista(id, lid);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }
}
