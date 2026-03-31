package dev.jms.app.module.cti.vonage.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vonage.client.users.UsersResponseException;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.helper.VoiceHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;
import java.util.HashMap;
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

    if (session.isAuthenticated()) {
      accountId = (int) session.sub();
      if (accountId > 0) {
        dao = new OperatorDAO(db);
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

    body = req.body();
    fromUser = DB.toString(body.get("from_user"));
    operatorUuid = DB.toString(body.get("uuid"));
    customDataStr = DB.toString(body.get("custom_data"));
    customerNumber = null;
    if (customDataStr != null && !customDataStr.isBlank()) {
      try {
        mapper = new ObjectMapper();
        customData = mapper.readValue(customDataStr, HashMap.class);
        customerNumber = DB.toString(customData.get("customerNumber"));
      } catch (Exception e) {
        log.warn("[CTI] answer: impossibile parsare custom_data: {}", customDataStr);
      }
    }

    conversationName = "call-" + UUID.randomUUID().toString();
    musicOnHoldUrl = config.get("cti.vonage.music_on_hold_url", "");
    nccoJson = voiceHelper.buildOperatorNccoJson(conversationName, musicOnHoldUrl);

    log.info("[CTI] answer: fromUser={}, operatorUuid={}, customerNumber={}", fromUser, operatorUuid, customerNumber);

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
        voiceHelper.callCustomer(customerNumber, conversationName, operatorUuid, db);
      } catch (Exception e) {
        log.error("[CTI] Errore avvio chiamata cliente: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * POST /api/cti/vonage/admin/operator — crea un utente Vonage e lo registra come operatore.
   *
   * <p>Crea l'utente nell'applicazione Vonage tramite {@code UsersClient} e inserisce
   * la riga corrispondente in {@code cti_operatori}. Il campo {@code name} diventa
   * il {@code vonage_user_id} (claim {@code sub} del JWT SDK).</p>
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   *
   * <p>Body JSON: {@code {"name": "operatore_01", "displayName": "Operatore 01"}}
   * — {@code displayName} è opzionale.</p>
   *
   * <p>Risposta: {@code {"vonageUserId": "...", "nome": "...", "attivo": true}}.</p>
   */
  public void createOperator(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String name;
    String displayName;
    String vonageUserId;
    OperatorDAO operatorDao;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    body = req.body();
    name = DB.toString(body.get("name"));
    displayName = DB.toString(body.get("displayName"));

    try {
      Validator.required(name, "name");
      vonageUserId = voiceHelper.createVonageUser(name, displayName);
      operatorDao = new OperatorDAO(db);
      operatorDao.insert(vonageUserId, displayName);
      out = new HashMap<>();
      out.put("vonageUserId", vonageUserId);
      out.put("nome", displayName);
      out.put("attivo", true);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    } catch (ValidationException | UsersResponseException e) {
      log.warn("[CTI] createOperator: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
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
  public void event(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String status;
    String uuid;

    body   = req.body();
    status = DB.toString(body.get("status"));
    uuid   = DB.toString(body.get("uuid"));

    log.info("[CTI] event: uuid={}, status={}", uuid, status);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }
}
