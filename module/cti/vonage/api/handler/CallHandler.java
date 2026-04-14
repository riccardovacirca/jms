package dev.jms.app.module.cti.vonage.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jms.app.module.cti.vonage.dao.CallDAO;
import dev.jms.app.module.cti.vonage.dao.CodaContattiDAO;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dao.OperatoreContattiDAO;
import dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO;
import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.app.module.cti.vonage.dto.OperatoreContattoDTO;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
   * POST /api/cti/vonage/sdk/auth — genera il JWT SDK per l'operatore permanentemente
   * assegnato all'account corrente.
   *
   * <p>Solo gli utenti con un operatore CTI permanentemente associato (colonna
   * {@code account_id} in {@code jms_cti_operatori}) possono connettersi.
   * Se non esiste un'associazione restituisce {@code err: true} con messaggio
   * "Utente non abilitato come operatore CTI".</p>
   *
   * <p>Il claim di sessione è idempotente: chiamate successive rinnovano
   * il TTL senza creare duplicati.</p>
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
    OperatoreContattiDAO opCodaDao;
    CodaContattiDAO codaDao;
    List<OperatoreContattoDTO> orphans;

    session.require(Role.USER, Permission.READ);
    accountId = (int) session.sub();
    dao = new OperatorDAO(db);
    operator = dao.findByAccountId(accountId);

    if (operator == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Utente non abilitato come operatore CTI")
         .out(null)
         .send();
    } else {
      opCodaDao = new OperatoreContattiDAO(db);
      orphans   = opCodaDao.findOrphans(operator.id());
      if (!orphans.isEmpty()) {
        codaDao = new CodaContattiDAO(db);
        db.begin();
        try {
          for (OperatoreContattoDTO orphan : orphans) {
            codaDao.insert(orphan.contattoJson());
            opCodaDao.rimuoviById(orphan.id());
          }
          db.commit();
        } catch (Exception e) {
          try { db.rollback(); } catch (Exception ignored) {}
          throw e;
        }
        log.info("[CTI] sdkToken: {} orfani reaccodati per operatore {} prima della connessione", orphans.size(), operator.id());
      }
      operator = dao.claimOrRenewAssigned(accountId, operator.id());
      userId = operator != null ? operator.vonageUserId() : null;
      if (userId == null || userId.isBlank()) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Operatore CTI non disponibile")
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
    OperatoreContattiDAO opCodaDao;
    CodaContattiDAO codaDao;
    List<OperatoreContattoDTO> orphans;

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

          opCodaDao = new OperatoreContattiDAO(db);
          orphans   = opCodaDao.findOrphans(operator.id());
          if (!orphans.isEmpty()) {
            codaDao = new CodaContattiDAO(db);
            db.begin();
            try {
              for (OperatoreContattoDTO orphan : orphans) {
                codaDao.insert(orphan.contattoJson());
                opCodaDao.rimuoviById(orphan.id());
              }
              db.commit();
            } catch (Exception e) {
              try { db.rollback(); } catch (Exception ignored) {}
              throw e;
            }
            log.info("[CTI] releaseSession: {} orfani reaccodati per operatore {}", orphans.size(), operator.id());
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
   * POST /api/cti/vonage/sdk/auth/listen — genera il JWT SDK per l'ascolto silenzioso.
   *
   * <p>Riservato agli account con ruolo {@code ADMIN} o {@code ROOT}. Restituisce lo stesso
   * JWT SDK di {@link #sdkToken} ma senza assegnare né reclamare un operatore CTI: l'admin
   * non ha bisogno di un operatore assegnato perché non avvia chiamate outbound.</p>
   *
   * <p>Il JWT generato consente al browser admin di connettersi al Vonage Client SDK e
   * di eseguire {@code client.serverCall({ listenTarget: "&lt;operatoreUuid&gt;" })} per
   * entrare silenziosamente in una conversazione attiva.</p>
   *
   * <p>Risposta: {@code {"token": "&lt;JWT RS256&gt;"}}.</p>
   */
  public void sdkAuthListen(HttpRequest req, HttpResponse res, Session session, DB db)
      throws Exception
  {
    String sdkJwt;
    HashMap<String, Object> out;
    String vonageUserId;

    session.require(Role.ADMIN, Permission.READ);
    vonageUserId = "admin-" + session.sub();
    sdkJwt = voiceHelper.generateSdkJwt(vonageUserId);
    out = new HashMap<>();
    out.put("token", sdkJwt);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * POST /api/cti/vonage/sdk/auth/release — rilascia l'operatore via {@code navigator.sendBeacon}.
   *
   * <p>Equivalente funzionale di {@link #releaseSession} ma accessibile tramite POST, poiché
   * {@code navigator.sendBeacon} invia sempre POST e non può usare DELETE. Viene chiamato
   * automaticamente dal browser durante il page unload (ricarica o chiusura tab), garantendo
   * che la richiesta sia inviata anche quando le Promise async di {@code disconnectedCallback}
   * verrebbero annullate dal browser prima del completamento.</p>
   *
   * <p>La logica interna è identica a {@link #releaseSession}: reaccodamento degli orfani,
   * chiusura della sessione tecnica e rilascio del claim sull'operatore.</p>
   */
  public void releaseBeacon(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    releaseSession(req, res, session, db);
  }

  /**
   * POST /api/cti/vonage/answer — webhook Vonage (answer URL).
   *
   * <p>Vonage chiama questo endpoint quando l'operatore esegue {@code serverCall()} via SDK.
   * Il campo {@code from_user} del body contiene il claim {@code sub} del JWT SDK dell'operatore.
   * Il {@code customerNumber} arriva nel campo {@code custom_data} del body JSON,
   * inoltrato da Vonage come stringa JSON serializzata dal dato passato al frontend
   * tramite {@code client.serverCall({ customerNumber: "..." })}.</p>
   *
   * <p>Risponde immediatamente con l'NCCO operatore (conversazione + registrazione), poi avvia
   * la chiamata al cliente con un ritardo di 1 secondo sullo stesso thread async.</p>
   *
   * <p>Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.
   * Registrato con {@code router.async()} poiché chiama l'API Vonage in modo sincrono
   * dopo aver inviato la risposta.</p>
   *
   * TODO: verificare cosa viene ascoltato dall'operatore durante l'attesa in assenza di musica configurata
   */
  @SuppressWarnings("unchecked")
  public void answer(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String fromUser;
    String customerNumber;
    String listenTarget;
    String operatorUuid;
    String conversationName;
    String musicOnHoldUrl;
    String eventUrl;
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
    log.debug("[CTI] answer: body={}", body);

    fromUser = DB.toString(body.get("from_user"));
    operatorUuid = DB.toString(body.get("uuid"));
    customDataStr = DB.toString(body.get("custom_data"));
    log.debug("[CTI] answer: custom_data grezzo={}", customDataStr);

    customerNumber = null;
    listenTarget   = null;
    contattoId     = null;
    callbackUrl    = null;
    if (customDataStr != null && !customDataStr.isBlank()) {
      try {
        mapper = new ObjectMapper();
        customData = mapper.readValue(customDataStr, HashMap.class);
        customerNumber = DB.toString(customData.get("customerNumber"));
        listenTarget   = DB.toString(customData.get("listenTarget"));
        contattoId = customData.get("contactId") != null ? DB.toLong(customData.get("contactId")) : null;
        callbackUrl = DB.toString(customData.get("callbackUrl"));
      } catch (Exception e) {
        log.warn("[CTI] answer: impossibile parsare custom_data: {}", customDataStr);
      }
    }

    if (listenTarget != null && !listenTarget.isBlank()) {
      // — branch ascolto silenzioso —
      // from_user è "admin-<accountId>" assegnato da sdkAuthListen; non è un operatore CTI
      log.info("[CTI] answer/listen: fromUser={}, listenTarget={}", fromUser, listenTarget);
      CallDAO callDao;
      CallDTO targetCall;
      callDao    = new CallDAO(db);
      targetCall = callDao.findByUuid(listenTarget);
      if (targetCall == null || targetCall.conversationName() == null) {
        log.warn("[CTI] answer/listen: chiamata target non trovata o senza conversation_name: {}", listenTarget);
        nccoJson = "[]";
      } else {
        nccoJson = voiceHelper.buildListenerNccoJson(targetCall.conversationName());
        log.info("[CTI] answer/listen: entro in conversazione={}", targetCall.conversationName());
      }
      res.status(200)
         .contentType("application/json")
         .raw(nccoJson);
    } else {
      // — branch chiamata normale —
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
      eventUrl = config.get("cti.vonage.event_url", "");
      nccoJson = voiceHelper.buildOperatorNccoJson(conversationName, musicOnHoldUrl, eventUrl);

      log.info("[CTI] answer: fromUser={}, operatoreId={}, operatorUuid={}, customerNumber={}",
               fromUser, operatoreId, operatorUuid, customerNumber);
      log.debug("[CTI] answer: NCCO={}", nccoJson);

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
   *
   * <p>Riceve eventi Voice e RTC da Vonage. Aggiorna {@code jms_cti_chiamate} in base
   * allo stato ricevuto ({@code answered}, {@code completed}, {@code recording}, errori).
   * Non richiede autenticazione: è un endpoint webhook raggiungibile solo da Vonage.</p>
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
   * GET /api/cti/vonage/call/active — chiamata attiva dell'operatore autenticato.
   *
   * <p>Interroga {@code jms_cti_chiamate} cercando una riga con stato {@code ringing} o
   * {@code answered} associata all'operatore dell'account corrente. Usato dal frontend al
   * reconnect per ripristinare lo stato della barra CTI dopo un page reload avvenuto durante
   * una conversazione.</p>
   *
   * <p>Risposta: {@code {"uuid": "...", "customerNumber": "...", "status": "ringing"|"answered"}}
   * oppure {@code null} se non esiste alcuna chiamata aperta.</p>
   */
  public void activeCall(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int accountId;
    OperatorDAO opDao;
    OperatorDTO operator;
    CallDAO callDao;
    HashMap<String, Object> row;
    HashMap<String, Object> out;

    session.require(Role.USER, Permission.READ);
    accountId = (int) session.sub();
    opDao     = new OperatorDAO(db);
    operator  = opDao.findByClaimAccountId(accountId);

    if (operator == null) {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
      return;
    }

    callDao = new CallDAO(db);
    row     = callDao.findActiveByOperatore(operator.id());

    if (row == null) {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
      return;
    }

    out = new HashMap<>();
    out.put("uuid",           row.get("uuid"));
    out.put("customerNumber", row.get("numero_destinatario"));
    out.put("status",         row.get("stato"));
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/cti/vonage/call/history — lista paginata delle chiamate.
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

  /**
   * GET /api/cti/vonage/call/{id}/recording — scarica e archivia la registrazione audio.
   *
   * <p>Legge {@code recording_url} da {@code jms_cti_chiamate}, scarica il file MP3 tramite
   * il Vonage Voice SDK, lo salva in storage nel percorso
   * {@code <basePath>/<operatoreId>/<YYYY-MM-DD>/<timestamp>_<id>.mp3} e aggiorna
   * {@code recording_path} sulla riga della chiamata.</p>
   *
   * <p>Il percorso base è configurabile tramite {@code cti.vonage.recordings.path}
   * (default: {@code /app/data/cti/recordings}).</p>
   *
   * <p>Richiede ruolo {@code ADMIN}. Risposta: {@code {"path": "<percorso locale>"}}.</p>
   */
  public void downloadRecording(HttpRequest req, HttpResponse res, Session session, DB db)
      throws Exception
  {
    String idParam;
    long id;
    CallDAO dao;
    CallDTO call;
    String recordingUrl;
    byte[] audio;
    String basePath;
    String dateDir;
    String filename;
    Path dir;
    Path file;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.READ);
    idParam = req.urlArgs().get("id");
    id      = Long.parseLong(idParam);
    dao     = new CallDAO(db);
    call    = dao.findById(id);

    if (call == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Chiamata non trovata")
         .out(null)
         .send();
      return;
    }

    recordingUrl = call.recordingUrl();
    if (recordingUrl == null || recordingUrl.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Registrazione non disponibile per questa chiamata")
         .out(null)
         .send();
      return;
    }

    audio    = voiceHelper.downloadRecordingRaw(recordingUrl);
    basePath = config.get("cti.vonage.recordings.path", "/app/data/cti/recordings");
    dateDir  = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    filename = System.currentTimeMillis() + "_" + id + ".mp3";

    if (call.operatoreId() != null) {
      dir = Paths.get(basePath, String.valueOf(call.operatoreId()), dateDir);
    } else {
      dir = Paths.get(basePath, "unknown", dateDir);
    }

    Files.createDirectories(dir);
    file = dir.resolve(filename);
    Files.write(file, audio);

    dao.updateRecordingPath(id, file.toString());
    log.info("[CTI] downloadRecording: id={} path={}", id, file);

    out = new HashMap<>();
    out.put("path", file.toString());
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

}
