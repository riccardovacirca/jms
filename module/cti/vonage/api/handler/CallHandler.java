package dev.jms.app.module.cti.vonage.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.module.cti.vonage.dao.CallDAO;
import dev.jms.app.module.cti.vonage.dto.CallDTO;
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
   * @param config configurazione applicazione (credenziali Vonage, cti.api.key)
   */
  public CallHandler(Config config)
  {
    this.config      = config;
    this.voiceHelper = new VoiceHelper(config);
  }

  /**
   * GET /api/cti/sdk-token — genera il JWT SDK per il Vonage Client SDK.
   * <p>Query param: {@code userId} — identificatore dell'operatore nel sistema Vonage.</p>
   * <p>Risposta: {@code {"token": "<JWT>"}}.</p>
   */
  public void sdkToken(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    String userId;
    String sdkToken;
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
        userId = req.getQueryParam("userId");
        if (userId == null || userId.isBlank()) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Parametro userId obbligatorio")
             .out(null)
             .send();
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
   * POST /api/cti/answer — webhook Vonage (answer URL).
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
    musicOnHoldUrl   = config.get("cti.vonage.music-on-hold-url", "");
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
   * PUT /api/cti/chiamate/{uuid}/hangup — riagancia la chiamata dell'operatore e del cliente.
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
   * GET /api/cti/chiamate — lista paginata delle chiamate.
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
