package dev.jms.app.module.cti.vonage.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.jms.app.module.cti.vonage.dao.CallDAO;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.helper.VoiceHelper;
import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
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

  private final Config      config;
  private final VoiceHelper voiceHelper;

  /**
   * @param config configurazione applicazione (credenziali Vonage, cti.vonage.api_key)
   */
  public CallHandler(Config config)
  {
    this.config      = config;
    this.voiceHelper = new VoiceHelper(config);
  }

  /**
   * POST /api/cti/vonage/sdk/auth — assegna un operatore e genera il JWT SDK.
   *
   * <p>Due modalità operative:</p>
   * <ul>
   *   <li><b>Con modulo user</b> (accountId &gt; 0): assegna dinamicamente un operatore
   *       libero dalla tabella {@code cti_operators} via claim atomico. Se l'account ha
   *       già un operatore assegnato lo restituisce direttamente (idempotente).</li>
   *   <li><b>Standalone</b> (accountId = 0, autenticazione via API key): richiede il
   *       parametro query {@code userId} passato esplicitamente.</li>
   * </ul>
   *
   * <p>Risposta: {@code {"token": "<JWT RS256>"}}.</p>
   */
  public void sdkToken(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String      token;
    DecodedJWT  decoded;
    int         accountId;
    String      userId;
    String      sdkToken;
    HashMap<String, Object> out;
    OperatorDAO dao;
    OperatorDTO operator;

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
        decoded   = Auth.get().verifyAccessToken(token);
        accountId = Integer.parseInt(decoded.getSubject());
        userId    = null;
        operator  = null;
        dao       = null;

        if (accountId > 0) {
          dao      = new OperatorDAO(db);
          operator = dao.claimOrRenew(accountId);
          if (operator != null) {
            userId = operator.vonageUserId();
          }
        } else {
          userId = req.getQueryParam("userId");
        }

        if (userId == null || userId.isBlank()) {
          if (accountId > 0) {
            res.status(200)
               .contentType("application/json")
               .err(true)
               .log("Nessun operatore CTI disponibile")
               .out(null)
               .send();
          } else {
            res.status(200)
               .contentType("application/json")
               .err(true)
               .log("Parametro userId obbligatorio")
               .out(null)
               .send();
          }
        } else {
          sdkToken = voiceHelper.generateSdkJwt(userId);
          out      = new HashMap<>();
          out.put("token", sdkToken);
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

  /**
   * DELETE /api/cti/vonage/sdk/auth — rilascia l'operatore assegnato alla sessione corrente.
   *
   * <p>Chiamato dal frontend al disconnect del componente. Operazione idempotente:
   * non restituisce errore se l'account non ha operatori assegnati o il token non è valido.</p>
   */
  public void releaseSession(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String      token;
    DecodedJWT  decoded;
    int         accountId;
    OperatorDAO dao;

    token = req.getCookie("access_token");
    if (token != null) {
      try {
        decoded   = Auth.get().verifyAccessToken(token);
        accountId = Integer.parseInt(decoded.getSubject());
        if (accountId > 0) {
          dao = new OperatorDAO(db);
          dao.releaseSession(accountId);
          log.info("[CTI] releaseSession: accountId={}", accountId);
        }
      } catch (JWTVerificationException e) {
        log.warn("[CTI] releaseSession: token non valido, nessun rilascio eseguito");
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
   * Il {@code customerNumber} arriva come query param, passato dal frontend tramite
   * {@code client.serverCall({ customerNumber: "..." })} e inoltrato da Vonage all'answer URL.<br>
   * Risponde immediatamente con l'NCCO operatore (musica di attesa), poi avvia la chiamata
   * al cliente con un ritardo di 1 secondo sullo stesso thread async.<br>
   * Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.
   * </p>
   *
   * <p>Registrato con {@code router.async()} poiché chiama l'API Vonage in modo sincrono
   * dopo aver inviato la risposta.</p>
   */
  @SuppressWarnings("unchecked")
  public void answer(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String fromUser;
    String customerNumber;
    String operatorUuid;
    String conversationName;
    String musicOnHoldUrl;
    String nccoJson;

    body           = req.body();
    fromUser       = DB.toString(body.get("from_user"));
    customerNumber = req.getQueryParam("customerNumber");
    operatorUuid   = DB.toString(body.get("uuid"));

    conversationName = "call-" + UUID.randomUUID().toString();
    musicOnHoldUrl   = config.get("cti.vonage.music_on_hold_url", "");
    nccoJson         = voiceHelper.buildOperatorNccoJson(conversationName, musicOnHoldUrl);

    log.info("[CTI] answer: fromUser={}, operatorUuid={}, customerNumber={}", fromUser, operatorUuid, customerNumber);

    res.status(200)
       .contentType("application/json")
       .raw(nccoJson);

    if (fromUser == null || fromUser.isBlank()) {
      log.error("[CTI] answer: from_user assente nel webhook - impossibile identificare l'operatore");
    } else if (customerNumber == null || customerNumber.isBlank()) {
      log.error("[CTI] answer: customerNumber assente nel query param - chiamata cliente NON avviata");
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
   * PUT /api/cti/vonage/call/{uuid}/hangup — riagancia la chiamata dell'operatore e del cliente.
   */
  public void hangup(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String uuid;

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
        uuid = req.urlArgs().get("uuid");
        voiceHelper.hangupCall(uuid);
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(null)
           .send();
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

  /**
   * GET /api/cti/vonage/chiamate — lista paginata delle chiamate.
   * <p>Query params: {@code page} (default 1), {@code size} (default 20).</p>
   */
  public void list(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String pageStr;
    String sizeStr;
    int page;
    int size;
    CallDAO dao;
    List<CallDTO> items;
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
        pageStr = req.getQueryParam("page");
        sizeStr = req.getQueryParam("size");
        page    = pageStr != null ? Integer.parseInt(pageStr) : 1;
        size    = sizeStr != null ? Integer.parseInt(sizeStr) : 20;
        dao     = new CallDAO(db);
        items   = dao.findAll(page, size);
        total   = dao.count();
        out     = new HashMap<>();
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
