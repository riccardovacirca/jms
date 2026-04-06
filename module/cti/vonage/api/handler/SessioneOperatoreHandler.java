package dev.jms.app.module.cti.vonage.handler;

import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.dto.SessioneOperatoreDTO;
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
 * Handler per la gestione dei turni operatore CTI ({@code jms_sessione_operatore}).
 *
 * <p>I turni sono creati e pianificati dall'admin. L'operatore li aggiorna
 * implicitamente tramite connessione/disconnessione.</p>
 */
public class SessioneOperatoreHandler
{
  private static final Log log = Log.get(SessioneOperatoreHandler.class);

  /**
   * POST /api/cti/vonage/admin/turno — crea un nuovo turno per un operatore.
   *
   * <p>Body JSON: {@code {"operatoreId": 1, "turnoInizio": "2026-04-05T09:00:00",
   * "turnoFine": "2026-04-05T17:00:00", "note": "..."}}.</p>
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
    SessioneOperatoreDAO dao;
    OperatorDAO opDao;
    OperatorDTO op;
    long newId;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    adminId = session.sub();
    body = req.body();

    try {
      Validator.required(body.get("operatoreId"), "operatoreId");
      Validator.required(body.get("turnoInizio"), "turnoInizio");
      Validator.required(body.get("turnoFine"),   "turnoFine");

      operatoreId   = DB.toLong(body.get("operatoreId"));
      turnoInizioStr = DB.toString(body.get("turnoInizio"));
      turnoFineStr   = DB.toString(body.get("turnoFine"));
      turnoInizio    = LocalDateTime.parse(turnoInizioStr);
      turnoFine      = LocalDateTime.parse(turnoFineStr);
      note           = DB.toString(body.get("note"));

      if (!turnoFine.isAfter(turnoInizio)) {
        throw new ValidationException("turnoFine deve essere successivo a turnoInizio");
      }

      opDao = new OperatorDAO(db);
      op    = opDao.findById(operatoreId);
      if (op == null) {
        throw new ValidationException("Operatore non trovato");
      }

      dao   = new SessioneOperatoreDAO(db);
      newId = dao.insert(operatoreId, turnoInizio, turnoFine, note, adminId);

      out = new HashMap<>();
      out.put("id", newId);
      log.info("[CTI] createTurno: id={}, operatoreId={}, adminId={}", newId, operatoreId, adminId);

      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    } catch (ValidationException e) {
      log.warn("[CTI] createTurno: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  /**
   * PUT /api/cti/vonage/admin/turno/{id} — aggiorna orari e note di un turno pianificato.
   *
   * <p>Consentito solo se il turno non è ancora iniziato (stato = 0 e connessione_inizio = NULL).</p>
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
    SessioneOperatoreDAO dao;
    SessioneOperatoreDTO turno;

    session.require(Role.ADMIN, Permission.WRITE);
    adminId = session.sub();
    id      = Long.parseLong(req.urlArgs().get("id"));
    dao     = new SessioneOperatoreDAO(db);
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

    if (turno.connessioneInizio() != null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Impossibile modificare un turno già avviato")
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
      log.warn("[CTI] updateTurno: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  /**
   * DELETE /api/cti/vonage/admin/turno/{id} — elimina un turno non ancora avviato.
   *
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    SessioneOperatoreDAO dao;
    SessioneOperatoreDTO turno;

    session.require(Role.ADMIN, Permission.WRITE);
    id    = Long.parseLong(req.urlArgs().get("id"));
    dao   = new SessioneOperatoreDAO(db);
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

    if (turno.connessioneInizio() != null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Impossibile eliminare un turno già avviato")
         .out(null)
         .send();
      return;
    }

    dao.delete(id);
    log.info("[CTI] deleteTurno: id={}", id);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * GET /api/cti/vonage/admin/turno — lista paginata di tutti i turni.
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
    List<SessioneOperatoreDTO> items;
    List<HashMap<String, Object>> out;
    SessioneOperatoreDAO dao;
    HashMap<String, Object> envelope;

    session.require(Role.ADMIN, Permission.READ);
    pageParam = req.queryParam("page");
    sizeParam = req.queryParam("size");
    page  = (pageParam != null && !pageParam.isBlank()) ? Integer.parseInt(pageParam) : 1;
    size  = (sizeParam != null && !sizeParam.isBlank()) ? Integer.parseInt(sizeParam) : 20;
    dao   = new SessioneOperatoreDAO(db);
    total = dao.count();
    items = dao.findAll(page, size);
    out   = new ArrayList<>();
    for (SessioneOperatoreDTO t : items) {
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
   * GET /api/cti/vonage/sessione/corrente — turno corrente dell'operatore autenticato.
   *
   * <p>Cerca il turno pianificato/in pausa con orario compatibile con NOW().
   * Risponde con il turno o {@code null} se nessun turno è attivo.</p>
   * <p>Richiede ruolo USER.</p>
   */
  public void corrente(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long accountId;
    OperatorDAO opDao;
    OperatorDTO op;
    SessioneOperatoreDAO dao;
    SessioneOperatoreDTO turno;

    session.require(Role.USER, Permission.READ);
    accountId = session.sub();
    opDao     = new OperatorDAO(db);
    op        = opDao.findByClaimAccountId((int) accountId);

    if (op == null) {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
      return;
    }

    dao   = new SessioneOperatoreDAO(db);
    turno = dao.findCorrente(op.id());
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(turno != null ? toMap(turno) : null)
       .send();
  }

  /** Converte un DTO in mappa serializzabile JSON. */
  private HashMap<String, Object> toMap(SessioneOperatoreDTO t)
  {
    HashMap<String, Object> m;

    m = new HashMap<>();
    m.put("id",                  t.id());
    m.put("operatoreId",         t.operatoreId());
    m.put("turnoInizio",         t.turnoInizio());
    m.put("turnoFine",           t.turnoFine());
    m.put("connessioneInizio",   t.connessioneInizio());
    m.put("connessioneFine",     t.connessioneFine());
    m.put("durataTotale",        t.durataTotale());
    m.put("numeroPause",         t.numeroPause());
    m.put("durataPause",         t.durataPause());
    m.put("ultimaConnessione",   t.ultimaConnessione());
    m.put("numeroChiamate",      t.numeroChiamate());
    m.put("durataConversazione", t.durataConversazione());
    m.put("stato",               t.stato());
    m.put("note",                t.note());
    m.put("creatoDA",            t.creatoDA());
    m.put("dataCreazione",       t.dataCreazione());
    m.put("modificatoDA",        t.modificatoDA());
    m.put("dataModifica",        t.dataModifica());
    return m;
  }
}
