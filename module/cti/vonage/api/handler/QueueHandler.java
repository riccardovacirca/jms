package dev.jms.app.module.cti.vonage.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jms.app.module.cti.vonage.dao.CodaChiamateDAO;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dto.CodaChiamateDTO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per la coda chiamate CTI.
 * Endpoint per inserire contatti (singoli/massivi), estrarre prossimo contatto, statistiche.
 */
public class QueueHandler
{
  private static final Log log = Log.get(QueueHandler.class);

  /**
   * POST /api/cti/vonage/queue — Inserisce un contatto nella coda.
   *
   * <p>Body: {@code {contattoJson: string, priorita?: number}}</p>
   * <p>Risposta: {@code {id: number}}</p>
   */
  public void addToQueue(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    String contattoJson;
    Integer priorita;
    CodaChiamateDAO dao;
    long id;
    HashMap<String, Object> body;
    HashMap<String, Object> out;

    body = req.body();
    contattoJson = (String) body.get("contattoJson");
    priorita = body.containsKey("priorita") ? DB.toInteger(body.get("priorita")) : 0;

    Validator.required(contattoJson, "contattoJson");

    dao = new CodaChiamateDAO(db);
    id = dao.insert(contattoJson, priorita);

    out = new HashMap<>();
    out.put("id", id);

    log.info("[CTI Queue] Contatto {} aggiunto alla coda (priorità {})", id, priorita);

    res.err(false).log(null).out(out).send();
  }

  /**
   * POST /api/cti/vonage/queue/bulk — Inserisce multipli contatti nella coda.
   *
   * <p>Body: {@code {contatti: string[], priorita?: number}}</p>
   * <p>Risposta: {@code {count: number}}</p>
   */
  public void addBulkToQueue(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);

    List<String> contatti;
    Integer priorita;
    CodaChiamateDAO dao;
    int count;
    HashMap<String, Object> body;
    HashMap<String, Object> out;

    body = req.body();
    contatti = (List<String>) body.get("contatti");
    priorita = body.containsKey("priorita") ? DB.toInteger(body.get("priorita")) : 0;

    if (contatti == null || contatti.isEmpty()) {
      throw new IllegalArgumentException("La lista contatti è obbligatoria e non può essere vuota");
    }

    dao = new CodaChiamateDAO(db);
    count = dao.insertBulk(contatti, priorita);

    out = new HashMap<>();
    out.put("count", count);

    log.info("[CTI Queue] {} contatti aggiunti alla coda in bulk (priorità {})", count, priorita);

    res.err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/queue/next — Estrae il prossimo contatto dalla coda e lo assegna all'operatore corrente.
   *
   * <p>Il contatto estratto viene assegnato all'operatore corrente sia nella coda
   * che nella colonna {@code contatto_corrente} della tabella operatori.</p>
   *
   * <p>Risposta: {@code {contatto: object}} o {@code null} se la coda è vuota.</p>
   */
  public void getNext(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    long operatoreId;
    CodaChiamateDAO codaDao;
    OperatorDAO operatorDao;
    CodaChiamateDTO codaItem;
    Map<String, Object> contatto;
    HashMap<String, Object> out;
    ObjectMapper mapper;

    operatoreId = (long) session.sub();

    codaDao = new CodaChiamateDAO(db);
    operatorDao = new OperatorDAO(db);

    codaItem = codaDao.getNext(operatoreId);

    if (codaItem == null) {
      log.warn("[CTI Queue] Coda vuota per operatore {}", operatoreId);
      res.err(false).log(null).out(null).send();
      return;
    }

    // Salva il contatto anche nella sessione operatore
    operatorDao.setContattoCorrente(operatoreId, codaItem.contattoJson());

    // Deserializza il JSON per la risposta
    mapper = new ObjectMapper();
    contatto = mapper.readValue(codaItem.contattoJson(), Map.class);

    out = new HashMap<>();
    out.put("contatto", contatto);
    out.put("codaId", codaItem.id());

    log.info("[CTI Queue] Contatto {} assegnato a operatore {}", codaItem.id(), operatoreId);

    res.err(false).log(null).out(out).send();
  }

  /**
   * GET /api/cti/vonage/queue/stats — Restituisce statistiche sulla coda.
   *
   * <p>Risposta: {@code {pending: number, assigned: number, completed: number, failed: number}}</p>
   */
  public void getStats(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.READ);

    long operatoreId;
    CodaChiamateDAO dao;
    Map<String, Integer> stats;
    HashMap<String, Object> out;
    boolean allStats;
    String allParam;

    operatoreId = (long) session.sub();
    allParam = req.queryParam("all");
    allStats = "true".equals(allParam) && session.ruoloLevel() >= Role.ADMIN.level();

    dao = new CodaChiamateDAO(db);
    stats = dao.getStats(allStats ? null : operatoreId);

    out = new HashMap<>();
    out.put("pending", stats.getOrDefault("pending", 0));
    out.put("assigned", stats.getOrDefault("assigned", 0));
    out.put("completed", stats.getOrDefault("completed", 0));
    out.put("failed", stats.getOrDefault("failed", 0));
    out.put("cancelled", stats.getOrDefault("cancelled", 0));
    out.put("scope", allStats ? "all" : "me");

    res.err(false).log(null).out(out).send();
  }
}
