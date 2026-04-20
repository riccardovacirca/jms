package dev.jms.app.crm.handler;

import dev.jms.app.crm.dao.TurnoDAO;
import dev.jms.app.crm.dto.TurnoDTO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handler per la gestione dei turni CRM ({@code jms_crm_turno}).
 *
 * <p>I turni vengono pianificati dall'admin. {@code operatoreId} fa riferimento
 * a {@code jms_cti_operatori.id} (dipendenza logica sul modulo cti/vonage).</p>
 */
public class TurnoHandler
{
  private static final Log log = Log.get(TurnoHandler.class);

  /**
   * GET /api/crm/operatori/turni — lista paginata di tutti i turni.
   *
   * <p>Query params: {@code page} (default 1), {@code size} (default 20).</p>
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int page;
    int size;
    int total;
    String pageParam;
    String sizeParam;
    List<TurnoDTO> items;
    List<HashMap<String, Object>> out;
    TurnoDAO dao;
    HashMap<String, Object> envelope;

    session.require(Role.ADMIN, Permission.READ);
    pageParam = req.queryParam("page");
    sizeParam = req.queryParam("size");
    page  = (pageParam != null && !pageParam.isBlank()) ? Integer.parseInt(pageParam) : 1;
    size  = (sizeParam != null && !sizeParam.isBlank()) ? Integer.parseInt(sizeParam) : 20;
    dao   = new TurnoDAO(db);
    total = dao.count();
    items = dao.findAll(page, size);
    out   = new ArrayList<>();
    for (TurnoDTO t : items) {
      out.add(toMap(t));
    }
    envelope = new HashMap<>();
    envelope.put("total", total);
    envelope.put("page",  page);
    envelope.put("size",  size);
    envelope.put("items", out);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(envelope)
       .send();
  }

  /**
   * POST /api/crm/operatori/turni — crea un nuovo turno per un operatore.
   *
   * <p>Body JSON: {@code {"operatoreId": 1, "turnoInizio": "2026-04-07T09:00:00",
   * "turnoFine": "2026-04-07T17:00:00", "note": "..."}}.</p>
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void create(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    long operatoreId;
    String turnoInizioStr;
    String turnoFineStr;
    LocalDateTime turnoInizio;
    LocalDateTime turnoFine;
    String note;
    long adminId;
    TurnoDAO dao;
    long newId;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    adminId = session.sub();
    body = req.body();

    try {
      Validator.required(DB.toString(body.get("operatoreId")), "operatoreId");
      Validator.required(DB.toString(body.get("turnoInizio")), "turnoInizio");
      Validator.required(DB.toString(body.get("turnoFine")),   "turnoFine");

      operatoreId    = DB.toLong(body.get("operatoreId"));
      turnoInizioStr = DB.toString(body.get("turnoInizio"));
      turnoFineStr   = DB.toString(body.get("turnoFine"));
      turnoInizio    = LocalDateTime.parse(turnoInizioStr);
      turnoFine      = LocalDateTime.parse(turnoFineStr);
      note           = DB.toString(body.get("note"));

      if (!turnoFine.isAfter(turnoInizio)) {
        throw new ValidationException("turnoFine deve essere successivo a turnoInizio");
      }

      dao   = new TurnoDAO(db);
      newId = dao.insert(operatoreId, turnoInizio, turnoFine, note, adminId);

      out = new HashMap<>();
      out.put("id", newId);
      log.info("[CRM] createTurno: id={}, operatoreId={}, adminId={}", newId, operatoreId, adminId);

      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    } catch (ValidationException e) {
      log.warn("[CRM] createTurno: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  /**
   * PUT /api/crm/operatori/turni/{id} — aggiorna orari e note di un turno.
   *
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    HashMap<String, Object> body;
    LocalDateTime turnoInizio;
    LocalDateTime turnoFine;
    String note;
    long adminId;
    TurnoDAO dao;
    TurnoDTO turno;

    session.require(Role.ADMIN, Permission.WRITE);
    adminId = session.sub();
    id      = Long.parseLong(req.urlArgs().get("id"));
    dao     = new TurnoDAO(db);
    turno   = dao.findById(id);

    if (turno == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Turno non trovato")
         .out(null)
         .send();
      return;
    }

    try {
      body        = req.body();
      turnoInizio = body.containsKey("turnoInizio")
          ? LocalDateTime.parse(DB.toString(body.get("turnoInizio"))) : turno.turnoInizio();
      turnoFine   = body.containsKey("turnoFine")
          ? LocalDateTime.parse(DB.toString(body.get("turnoFine"))) : turno.turnoFine();
      note        = body.containsKey("note") ? DB.toString(body.get("note")) : turno.note();

      if (!turnoFine.isAfter(turnoInizio)) {
        throw new ValidationException("turnoFine deve essere successivo a turnoInizio");
      }

      dao.update(id, turnoInizio, turnoFine, note, adminId);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    } catch (ValidationException e) {
      log.warn("[CRM] updateTurno: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  /**
   * DELETE /api/crm/operatori/turni/{id} — elimina un turno.
   *
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    TurnoDAO dao;
    TurnoDTO turno;

    session.require(Role.ADMIN, Permission.WRITE);
    id    = Long.parseLong(req.urlArgs().get("id"));
    dao   = new TurnoDAO(db);
    turno = dao.findById(id);

    if (turno == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Turno non trovato")
         .out(null)
         .send();
      return;
    }

    dao.delete(id);
    log.info("[CRM] deleteTurno: id={}", id);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * GET /api/crm/operatori/turni/corrente — turno attivo dell'operatore autenticato.
   *
   * <p>Cerca il turno con orario compatibile con NOW() per l'operatore CTI
   * associato all'account corrente. Risponde con il turno o {@code null}.</p>
   * <p>Richiede ruolo USER.</p>
   */
  public void corrente(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long accountId;
    String sql;
    List<HashMap<String, Object>> rows;
    Long operatoreId;
    TurnoDAO dao;
    TurnoDTO turno;

    session.require(Role.USER, Permission.READ);
    accountId   = session.sub();
    operatoreId = null;

    sql  = "SELECT id FROM jms_cti_operatori WHERE claim_account_id = ?";
    rows = db.select(sql, accountId);
    if (!rows.isEmpty()) {
      operatoreId = DB.toLong(rows.get(0).get("id"));
    }

    if (operatoreId == null) {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
      return;
    }

    dao   = new TurnoDAO(db);
    turno = dao.findCorrente(operatoreId);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(turno != null ? toMap(turno) : null)
       .send();
  }

  /** Converte un DTO in mappa serializzabile JSON. */
  private HashMap<String, Object> toMap(TurnoDTO t)
  {
    HashMap<String, Object> m;

    m = new HashMap<>();
    m.put("id",            t.id());
    m.put("operatoreId",   t.operatoreId());
    m.put("turnoInizio",   t.turnoInizio());
    m.put("turnoFine",     t.turnoFine());
    m.put("note",          t.note());
    m.put("creatoDA",      t.creatoDA());
    m.put("dataCreazione", t.dataCreazione());
    m.put("modificatoDA",  t.modificatoDA());
    m.put("dataModifica",  t.dataModifica());
    return m;
  }
}
