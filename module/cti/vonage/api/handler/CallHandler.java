package dev.jms.app.module.cti.vonage.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jms.app.module.cti.vonage.dao.CallDAO;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.dto.SessioneOperatoreDTO;
import dev.jms.app.module.cti.vonage.helper.VoiceHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Handler per le operazioni CTI: gestione chiamate e sessione SDK operatore.
 * Implementa il pattern operator-first progressive dialer tramite {@link VoiceHelper}.
 */
public class CallHandler
{
  private static final Log log = Log.get(CallHandler.class);

  private final Config config;
  private final VoiceHelper voiceHelper;

  /**
   * @param config configurazione applicazione (credenziali Vonage)
   */
  public CallHandler(Config config)
  {
    this.config = config;
    this.voiceHelper = new VoiceHelper(config);
  }

  /**
   * POST /api/cti/vonage/sdk/auth — assegna un operatore e genera il JWT SDK.
   *
   * <p>Assegna dinamicamente un operatore libero dalla tabella {@code cti_operatori}
   * via claim atomico. Se l'account ha già un operatore assegnato lo restituisce
   * direttamente (idempotente).</p>
   *
   * <p>Risposta: {@code {"token": "<JWT RS256>"}}.</p>
   */
  public void sdkToken(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int accountId;
    String userId;
    String sdkToken;
    HashMap<String, Object> out;
    OperatorDAO dao;
    OperatorDTO operator;
    SessioneOperatoreDAO sessioneDao;
    SessioneOperatoreDTO sessione;
    long sessioneId;

    session.require(Role.USER, Permission.READ);
    accountId = (int) session.sub();
    userId = null;
    dao = new OperatorDAO(db);
    operator = dao.claimOrRenew(accountId);

    if (operator != null) {
      userId = operator.vonageUserId();
    }

    if (userId == null || userId.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Nessun operatore CTI disponibile")
         .out(null)
         .send();
    } else {
      sessioneDao = new SessioneOperatoreDAO(db);
      sessione = sessioneDao.findActive(operator.id());
      if (sessione != null) {
        sessioneId = sessione.id();
      } else {
        sessioneId = sessioneDao.openSession(operator.id(), accountId);
      }
      sessioneDao.registraConnessione(sessioneId, accountId);
      sdkToken = voiceHelper.generateSdkJwt(userId);
      out = new HashMap<>();
      out.put("token", sdkToken);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    }
  }

  /**
   * DELETE /api/cti/vonage/sdk/auth — rilascia l'operatore assegnato alla sessione corrente.
   *
   * <p>Chiamato dal frontend al disconnect del componente. Operazione idempotente.</p>
   */
  public void releaseSession(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int accountId;
    OperatorDAO dao;
    OperatorDTO operator;
    SessioneOperatoreDAO sessioneDao;
    SessioneOperatoreDTO sessione;
    int durataPausa;

    if (session.isAuthenticated()) {
      accountId = (int) session.sub();
      if (accountId > 0) {
        dao      = new OperatorDAO(db);
        operator = dao.findByClaimAccountId(accountId);
        if (operator != null) {
          sessioneDao = new SessioneOperatoreDAO(db);
          sessione    = sessioneDao.findActive(operator.id());
          if (sessione != null) {
            durataPausa = sessione.ultimaConnessione() != null
                ? (int) java.time.Duration.between(
                    sessione.ultimaConnessione(), java.time.LocalDateTime.now()).getSeconds()
                : 0;
            sessioneDao.registraPausa(sessione.id(), durataPausa, accountId);
          }
        }
        dao.releaseSession(accountId);
        log.info("[CTI] releaseSession: accountId={}", accountId);
      }
    }
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * POST /api/cti/vonage/answer — webhook Vonage (answer URL).
   * <p>
   * Vonage chiama questo endpoint quando l'operatore esegue {@code serverCall()} via SDK.
   * Il campo {@code from_user} del body contiene il claim {@code sub} del JWT SDK dell'operatore.
   * Il {@code customerNumber} arriva nel campo {@code custom_data} del body JSON,
   * inoltrato da Vonage come stringa JSON serializzata dal dato passato al frontend
   * tramite {@code client.serverCall({ customerNumber: "..." })}.<br>
   * Risponde immediatamente con l'NCCO operatore (musica di attesa), poi avvia la chiamata
   * al cliente con un ritardo di 1 secondo sullo stesso thread async.<br>
   * Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.
   * </p>
   *
   * <p>Registrato con {@code router.async()} poiché chiama l'API Vonage in modo sincrono
   * dopo aver inviato la risposta.</p>
   * TODO: verificare cosa viene ascoltato dall'operatore durante l'attesa in assenza di musica configurata
   */
  @SuppressWarnings("unchecked")
  public void answer(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String fromUser;
    String customerNumber;
    String operatorUuid;
    String conversationName;
    String musicOnHoldUrl;
    String nccoJson;
    String customDataStr;
    HashMap customData;
    ObjectMapper mapper;
    OperatorDAO opDao;
    OperatorDTO op;
    Long operatoreId;
    Long chiamanteAccountId;
    Long contattoId;
    String callbackUrl;

    body = req.body();
    fromUser = DB.toString(body.get("from_user"));
    operatorUuid = DB.toString(body.get("uuid"));
    customDataStr = DB.toString(body.get("custom_data"));
    customerNumber = null;
    contattoId = null;
    callbackUrl = null;
    if (customDataStr != null && !customDataStr.isBlank()) {
      try {
        mapper = new ObjectMapper();
        customData = mapper.readValue(customDataStr, HashMap.class);
        customerNumber = DB.toString(customData.get("customerNumber"));
        contattoId = customData.get("contactId") != null ? DB.toLong(customData.get("contactId")) : null;
        callbackUrl = DB.toString(customData.get("callbackUrl"));
      } catch (Exception e) {
        log.warn("[CTI] answer: impossibile parsare custom_data: {}", customDataStr);
      }
    }

    operatoreId = null;
    chiamanteAccountId = null;
    if (fromUser != null && !fromUser.isBlank()) {
      opDao = new OperatorDAO(db);
      op = opDao.findByVonageUserId(fromUser);
      if (op != null) {
        operatoreId = op.id();
        chiamanteAccountId = opDao.findSessionAccountId(operatoreId);
      }
    }

    conversationName = "call-" + UUID.randomUUID().toString();
    musicOnHoldUrl = config.get("cti.vonage.music_on_hold_url", "");
    nccoJson = voiceHelper.buildOperatorNccoJson(conversationName, musicOnHoldUrl);

    log.info("[CTI] answer: fromUser={}, operatoreId={}, operatorUuid={}, customerNumber={}",
             fromUser, operatoreId, operatorUuid, customerNumber);

    res.status(200)
       .contentType("application/json")
       .raw(nccoJson);

    if (fromUser == null || fromUser.isBlank()) {
      log.error("[CTI] answer: from_user assente nel webhook - impossibile identificare l'operatore");
    } else if (customerNumber == null || customerNumber.isBlank()) {
      log.error("[CTI] answer: customerNumber assente in custom_data - chiamata cliente NON avviata");
    } else if (operatorUuid == null || operatorUuid.isBlank()) {
      log.error("[CTI] answer: operatorUuid assente nel webhook - chiamata cliente NON avviata");
    } else {
      try {
        Thread.sleep(1000);
        voiceHelper.callCustomer(customerNumber, conversationName, operatorUuid,
                                 operatoreId, chiamanteAccountId, contattoId, callbackUrl, db);
      } catch (Exception e) {
        log.error("[CTI] Errore avvio chiamata cliente: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * PUT /api/cti/vonage/call/{uuid}/hangup — riagancia la chiamata dell'operatore e del cliente.
   */
  public void hangup(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String uuid;

    session.require(Role.USER, Permission.WRITE);
    uuid = req.urlArgs().get("uuid");
    voiceHelper.hangupCall(uuid);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * POST /api/cti/vonage/event — webhook Vonage (event URL).
   * <p>
   * Riceve eventi Voice e RTC da Vonage (call state changes, WebRTC session events).
   * Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.
   * </p>
   */
  /**
   * POST /api/cti/vonage/event — webhook Vonage (event URL).
   * <p>
   * Riceve eventi Voice e RTC da Vonage. Aggiorna {@code jms_chiamate} in base
   * allo stato ricevuto (answered, completed, errori).
   * Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.
   * </p>
   */
  public void event(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String status;
    String uuid;

    body   = req.body();
    status = DB.toString(body.get("status"));
    uuid   = DB.toString(body.get("uuid"));

    log.info("[CTI] event: uuid={}, status={}", uuid, status);

    try {
      voiceHelper.processEvent(body, db);
    } catch (Exception e) {
      log.error("[CTI] Errore processEvent: {}", e.getMessage(), e);
    }

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * GET /api/cti/vonage/call — lista paginata delle chiamate.
   *
   * <p>Query params: {@code page} (default 1), {@code size} (default 20).</p>
   * <p>Risposta: {@code {"total": n, "page": p, "size": s, "items": [...]}}.</p>
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String pageParam;
    String sizeParam;
    int page;
    int size;
    int total;
    List<HashMap<String, Object>> items;
    HashMap<String, Object> out;
    CallDAO dao;
    boolean isAdmin;
    long accountId;

    session.require(Role.USER, Permission.READ);
    pageParam = req.queryParam("page");
    sizeParam = req.queryParam("size");
    page     = (pageParam != null && !pageParam.isBlank()) ? Integer.parseInt(pageParam) : 1;
    size     = (sizeParam != null && !sizeParam.isBlank()) ? Integer.parseInt(sizeParam) : 20;
    isAdmin  = session.ruoloLevel() >= Role.ADMIN.level();
    dao      = new CallDAO(db);

    if (isAdmin) {
      total = dao.count();
      items = dao.findAllForApi(page, size);
    } else {
      accountId = session.sub();
      total     = dao.countByAccount(accountId);
      items     = dao.findByAccountForApi(page, size, accountId);
    }

    out = new HashMap<>();
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
