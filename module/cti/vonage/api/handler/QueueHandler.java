package dev.jms.app.module.cti.vonage.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jms.app.module.cti.vonage.dao.CodaContattiDAO;
import dev.jms.app.module.cti.vonage.dao.OperatoreContattiDAO;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dto.OperatoreContattoDTO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.Validator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per la coda contatti CTI.
 *
 * <p>Gestisce l'inserimento nella coda globale, l'estrazione verso la coda personale
 * dell'operatore, la pianificazione di richiamate future e la rimozione dopo la chiamata.</p>
 */
public class QueueHandler
{
  private static final Log log = Log.get(QueueHandler.class);

  /**
   * POST /api/cti/vonage/queue — Inserisce un contatto nella coda globale.
   *
   * <p>Body: {@code {contattoJson: string}}</p>
   * <p>Risposta: {@code {id: number}}</p>
   */
  public void addToQueue(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    String contattoJson;
    CodaContattiDAO dao;
    long id;
    HashMap<String, Object> body;
    HashMap<String, Object> out;

    body = req.body();
    contattoJson = (String) body.get("contattoJson");

    Validator.required(contattoJson, "contattoJson");

    dao = new CodaContattiDAO(db);
    id = dao.insert(contattoJson);

    out = new HashMap<>();
    out.put("id", id);

    log.info("[CTI Queue] Contatto {} aggiunto alla coda globale", id);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * POST /api/cti/vonage/queue/bulk — Inserisce multipli contatti nella coda globale.
   *
   * <p>Body: {@code {contatti: string[]}}</p>
   * <p>Risposta: {@code {count: number}}</p>
   */
  public void addBulkToQueue(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    List<String> contatti;
    CodaContattiDAO dao;
    int count;
    HashMap<String, Object> body;
    HashMap<String, Object> out;

    body = req.body();
    contatti = (List<String>) body.get("contatti");

    if (contatti == null || contatti.isEmpty()) {
      throw new IllegalArgumentException("La lista contatti è obbligatoria e non può essere vuota");
    }

    dao = new CodaContattiDAO(db);
    count = dao.insertBulk(contatti);

    out = new HashMap<>();
    out.put("count", count);

    log.info("[CTI Queue] {} contatti aggiunti alla coda globale in bulk", count);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/queue/next — Restituisce il prossimo contatto disponibile per l'operatore.
   *
   * <p>Prima cerca nella coda personale dell'operatore (contatti con pianificato_al &lt;= NOW());
   * se vuota, estrae il prossimo dalla coda globale e lo assegna all'operatore.</p>
   *
   * <p>Risposta: {@code {contatto: object, codaId: number}} o {@code null} se non ci sono contatti.</p>
   */
  public void getNext(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    OperatorDTO operator;
    OperatoreContattiDAO dao;
    OperatoreContattoDTO item;
    Map<String, Object> contatto;
    HashMap<String, Object> out;
    ObjectMapper mapper;
    long operatoreId;

    operator = new OperatorDAO(db).findByClaimAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Sessione operatore non attiva").out(null).send();
      return;
    }

    operatoreId = operator.id();
    dao = new OperatoreContattiDAO(db);
    item = dao.getNext(operatoreId);

    if (item == null) {
      log.warn("[CTI Queue] Coda vuota per operatore {}", operatoreId);
      res.status(200).contentType("application/json")
         .err(false).log(null).out(null).send();
      return;
    }

    mapper = new ObjectMapper();
    contatto = mapper.readValue(item.contattoJson(), Map.class);

    out = new HashMap<>();
    out.put("contatto", contatto);
    out.put("codaId", item.id());

    log.info("[CTI Queue] Contatto {} proposto a operatore {}", item.id(), operatoreId);
    log.debug("[CTI Queue] Contatto {}: phone={}", item.id(), contatto.get("phone"));

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/queue/contact — Restituisce il primo contatto disponibile nella coda personale.
   *
   * <p>Usato al reconnect del frontend per ripristinare un contatto senza riestrarre dalla coda globale.
   * Restituisce {@code null} se l'operatore non ha contatti disponibili o la sessione non è attiva.</p>
   *
   * <p>Risposta: {@code {contatto: object, codaId: number}} o {@code null}.</p>
   */
  public void getCurrentContact(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    OperatorDTO operator;
    OperatoreContattiDAO dao;
    OperatoreContattoDTO item;
    HashMap<String, Object> out;
    Map<String, Object> contatto;
    ObjectMapper mapper;
    long operatoreId;

    operator = new OperatorDAO(db).findByClaimAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(false).log(null).out(null).send();
      return;
    }

    operatoreId = operator.id();
    dao = new OperatoreContattiDAO(db);
    item = dao.getFirstAvailable(operatoreId);

    if (item == null) {
      res.status(200).contentType("application/json")
         .err(false).log(null).out(null).send();
      return;
    }

    mapper = new ObjectMapper();
    contatto = mapper.readValue(item.contattoJson(), Map.class);

    out = new HashMap<>();
    out.put("contatto", contatto);
    out.put("codaId", item.id());

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * PUT /api/cti/vonage/queue/contatto/{id}/pianifica — Pianifica un contatto per un richiamo futuro.
   *
   * <p>Body: {@code {pianificatoAl: string}} (ISO 8601, es. {@code 2026-04-12T09:30:00})</p>
   * <p>Risposta: {@code null}</p>
   */
  public void pianifica(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    OperatorDTO operator;
    OperatoreContattiDAO dao;
    HashMap<String, Object> body;
    String pianificatoAlStr;
    LocalDateTime pianificatoAl;
    long id;
    long operatoreId;

    operator = new OperatorDAO(db).findByClaimAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Sessione operatore non attiva").out(null).send();
      return;
    }

    id          = Long.parseLong(req.urlArgs().get("id"));
    operatoreId = operator.id();
    body        = req.body();
    pianificatoAlStr = (String) body.get("pianificatoAl");

    Validator.required(pianificatoAlStr, "pianificatoAl");

    pianificatoAl = LocalDateTime.parse(pianificatoAlStr);

    dao = new OperatoreContattiDAO(db);
    dao.pianifica(id, operatoreId, pianificatoAl);

    log.info("[CTI Queue] Contatto {} pianificato al {} per operatore {}", id, pianificatoAl, operatoreId);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(null).send();
  }

  /**
   * DELETE /api/cti/vonage/queue/contatto/{id} — Rimuove un contatto dalla coda personale (contatto chiamato).
   *
   * <p>Risposta: {@code null}</p>
   */
  public void rimuoviContatto(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    OperatorDTO operator;
    OperatoreContattiDAO dao;
    long id;
    long operatoreId;

    operator = new OperatorDAO(db).findByClaimAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Sessione operatore non attiva").out(null).send();
      return;
    }

    id          = Long.parseLong(req.urlArgs().get("id"));
    operatoreId = operator.id();

    dao = new OperatoreContattiDAO(db);
    dao.rimuovi(id, operatoreId);

    log.info("[CTI Queue] Contatto {} rimosso dalla coda personale operatore {}", id, operatoreId);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(null).send();
  }

  /**
   * GET /api/cti/vonage/queue/stats — Restituisce statistiche sulla coda.
   *
   * <p>Query param {@code all=true} (solo admin): mostra i totali su tutti gli operatori.</p>
   * <p>Risposta: {@code {inCoda: number, disponibili: number, pianificati: number, scope: string}}</p>
   */
  public void getStats(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    OperatorDTO operator;
    CodaContattiDAO codaDao;
    OperatoreContattiDAO opDao;
    Map<String, Integer> opStats;
    HashMap<String, Object> out;
    Long operatoreId;
    boolean allStats;

    allStats = "true".equals(req.queryParam("all")) && session.ruoloLevel() >= Role.ADMIN.level();

    if (allStats) {
      operatoreId = null;
    } else {
      operator = new OperatorDAO(db).findByClaimAccountId((int) session.sub());
      if (operator == null) {
        res.status(200).contentType("application/json")
           .err(true).log("Sessione operatore non attiva").out(null).send();
        return;
      }
      operatoreId = operator.id();
    }

    codaDao  = new CodaContattiDAO(db);
    opDao    = new OperatoreContattiDAO(db);
    opStats  = opDao.countByStato(operatoreId);

    out = new HashMap<>();
    out.put("inCoda",      codaDao.count());
    out.put("disponibili", opStats.getOrDefault("disponibili", 0));
    out.put("pianificati", opStats.getOrDefault("pianificati", 0));
    out.put("scope",       allStats ? "all" : "me");

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/admin/operator/{id}/queue — Coda personale di un operatore (admin).
   *
   * <p>Restituisce tutti i contatti presenti nella coda personale dell'operatore,
   * indipendentemente da {@code pianificato_al}. Usato dalla dashboard admin per
   * il monitoraggio e il cleanup degli orfani.</p>
   *
   * <p>Risposta: array di {@code {id, contatto, dataInserimento, pianificatoAl, disponibile}}.</p>
   */
  public void adminQueueByOperator(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.ADMIN, Permission.READ);

    long operatoreId;
    OperatoreContattiDAO dao;
    List<dev.jms.app.module.cti.vonage.dto.OperatoreContattoDTO> items;
    List<HashMap<String, Object>> out;
    ObjectMapper mapper;
    java.time.LocalDateTime now;

    operatoreId = Long.parseLong(req.urlArgs().get("id"));
    dao         = new OperatoreContattiDAO(db);
    items       = dao.findByOperatore(operatoreId);
    mapper      = new ObjectMapper();
    now         = java.time.LocalDateTime.now();
    out         = new java.util.ArrayList<>();

    for (dev.jms.app.module.cti.vonage.dto.OperatoreContattoDTO item : items) {
      HashMap<String, Object> entry;
      Object contatto;

      entry = new HashMap<>();
      entry.put("id", item.id());
      entry.put("dataInserimento", item.dataInserimento() != null ? item.dataInserimento().toString() : null);
      entry.put("pianificatoAl",   item.pianificatoAl()   != null ? item.pianificatoAl().toString()   : null);
      entry.put("disponibile",     item.pianificatoAl() == null || !item.pianificatoAl().isAfter(now));
      try {
        contatto = mapper.readValue(item.contattoJson(), Object.class);
      } catch (Exception e) {
        contatto = item.contattoJson();
      }
      entry.put("contatto", contatto);
      out.add(entry);
    }

    log.info("[CTI] adminQueueByOperator: operatoreId={}, count={}", operatoreId, out.size());

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/queue/contatti — Coda personale dell'operatore autenticato.
   *
   * <p>Restituisce tutti i contatti nella coda personale dell'operatore connesso,
   * sia disponibili che pianificati. Usato per la modal "Coda chiamate".</p>
   *
   * <p>Risposta: array di {@code {id, contatto, dataInserimento, pianificatoAl, disponibile}}.</p>
   */
  public void listPersonal(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    OperatorDTO operator;
    OperatoreContattiDAO dao;
    List<OperatoreContattoDTO> items;
    List<HashMap<String, Object>> out;
    ObjectMapper mapper;
    java.time.LocalDateTime now;

    operator = new OperatorDAO(db).findByAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Nessun operatore assegnato all'account").out(null).send();
      return;
    }

    dao    = new OperatoreContattiDAO(db);
    items  = dao.findByOperatore(operator.id());
    mapper = new ObjectMapper();
    now    = java.time.LocalDateTime.now();
    out    = new java.util.ArrayList<>();

    for (OperatoreContattoDTO item : items) {
      HashMap<String, Object> entry;
      Object contatto;

      entry = new HashMap<>();
      entry.put("id",              item.id());
      entry.put("dataInserimento", item.dataInserimento() != null ? item.dataInserimento().toString() : null);
      entry.put("pianificatoAl",   item.pianificatoAl()   != null ? item.pianificatoAl().toString()   : null);
      entry.put("disponibile",     item.pianificatoAl() == null || !item.pianificatoAl().isAfter(now));
      try {
        contatto = mapper.readValue(item.contattoJson(), Object.class);
      } catch (Exception e) {
        contatto = item.contattoJson();
      }
      entry.put("contatto", contatto);
      out.add(entry);
    }

    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /**
   * DELETE /api/cti/vonage/queue/contatto/{id}/rimetti — Rimette in coda globale (operatore).
   *
   * <p>Reinserisce il contatto nella coda globale ({@code jms_cti_coda_contatti}) e poi
   * lo elimina dalla coda personale dell'operatore, in modo atomico.
   * Verifica che il contatto appartenga all'operatore autenticato.</p>
   *
   * <p>Risposta: {@code null}</p>
   */
  public void rimettiInCoda(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    OperatorDTO operator;
    long id;
    long operatoreId;
    String contattoJson;
    OperatoreContattiDAO opDao;
    CodaContattiDAO codaDao;
    String sql;
    List<HashMap<String, Object>> rows;

    operator = new OperatorDAO(db).findByAccountId((int) session.sub());

    if (operator == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Nessun operatore assegnato all'account").out(null).send();
      return;
    }

    id          = Long.parseLong(req.urlArgs().get("id"));
    operatoreId = operator.id();

    sql  = "SELECT contatto_json FROM jms_cti_operatore_contatti WHERE id = ? AND operatore_id = ?";
    rows = db.select(sql, id, operatoreId);

    if (rows.isEmpty()) {
      res.status(200).contentType("application/json")
         .err(true).log("Contatto non trovato nella coda personale").out(null).send();
      return;
    }

    contattoJson = DB.toString(rows.get(0).get("contatto_json"));
    opDao        = new OperatoreContattiDAO(db);
    codaDao      = new CodaContattiDAO(db);

    db.begin();
    try {
      codaDao.insert(contattoJson);
      opDao.rimuoviById(id);
      db.commit();
    } catch (Exception e) {
      try { db.rollback(); } catch (Exception ignored) {}
      throw e;
    }

    log.info("[CTI Queue] Contatto {} rimesso in coda globale da operatore {}", id, operatoreId);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(null).send();
  }

  /**
   * DELETE /api/cti/vonage/admin/queue/contatto/{id} — Rimette in coda globale (admin).
   *
   * <p>Reinserisce il contatto nella coda globale ({@code jms_cti_coda_contatti}) e poi
   * lo elimina dalla coda personale dell'operatore, in modo atomico.
   * Usato per il recupero manuale degli orfani dalla dashboard.</p>
   *
   * <p>Risposta: {@code null}</p>
   */
  public void adminRimuoviContatto(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.ADMIN, Permission.WRITE);

    long id;
    OperatoreContattiDAO opDao;
    CodaContattiDAO codaDao;
    String contattoJson;

    id    = Long.parseLong(req.urlArgs().get("id"));
    opDao = new OperatoreContattiDAO(db);

    // recupera il contattoJson tramite una query diretta per id
    contattoJson = null;
    {
      List<java.util.HashMap<String, Object>> rows;
      String sql;
      sql  = "SELECT contatto_json FROM jms_cti_operatore_contatti WHERE id = ?";
      rows = db.select(sql, id);
      if (!rows.isEmpty()) {
        contattoJson = DB.toString(rows.get(0).get("contatto_json"));
      }
    }

    if (contattoJson == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Contatto non trovato").out(null).send();
      return;
    }

    db.begin();
    try {
      codaDao = new CodaContattiDAO(db);
      codaDao.insert(contattoJson);
      opDao.rimuoviById(id);
      db.commit();
    } catch (Exception e) {
      try { db.rollback(); } catch (Exception ignored) {}
      throw e;
    }

    log.info("[CTI] adminRimuoviContatto: id={} reinserito in coda globale", id);

    res.status(200).contentType("application/json")
       .err(false).log(null).out(null).send();
  }
}
