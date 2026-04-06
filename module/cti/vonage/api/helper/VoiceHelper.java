package dev.jms.app.module.cti.vonage.helper;

import com.vonage.client.VonageClient;
import com.vonage.client.users.BaseUser;
import com.vonage.client.users.User;
import com.vonage.client.users.UsersResponseException;
import com.vonage.client.voice.Call;
import com.vonage.client.voice.CallEvent;
import com.vonage.client.voice.ncco.ConversationAction;
import com.vonage.jwt.Jwt;
import dev.jms.app.module.cti.vonage.dao.CallDAO;
import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Json;
import dev.jms.util.Log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic del modulo CTI
 * Gestione del flusso operator-first progressive dialer
 * tramite il Vonage Java Server SDK.
 *
 * <p>Mantiene una mappa in-memory thread-safe:</p>
 * <ul>
 * <li>  {@code outgoingCalls}: operatorUuid/customerUuid
 *        (per hangup simultaneo)</li>
 * </ul>
 *
 * <p>Flusso applicativo:</p>
 * <ol>
 * <li> Frontend esegue {@code client.serverCall({ customerNumber })}
 *      via Vonage Client SDK</li>
 * <li> Vonage chiama {@code POST /api/cti/answer?customerNumber=...}
 *      handler risponde con NCCO operatore</li>
 * <li> Dopo 1s {@link #callCustomer} chiama il cliente
 *      tramite Vonage Voice API</li>
 * <li> Per riagganciare {@link #hangupCall} termina entrambe le chiamate</li>
 * </ol>
 */
public class VoiceHelper
{
  // Istanza del logger associata alla classe VoiceHelper
  private static final Log log = Log.get(VoiceHelper.class);
  private final Config config;
  private final VonageClient vonageClient;
  private final String applicationId;
  private final String privateKeyPath;

  /**
   * Mapping operatore/cliente per hangup simultaneo: operatorUuid → customerUuid.
   */
  private final Map<String, String> outgoingCalls = new ConcurrentHashMap<>();

  /**
   * Operatori per cui è stato richiesto hangup prima che {@link #callCustomer}
   * completasse la registrazione in {@link #outgoingCalls}. Quando
   * {@link #callCustomer} ottiene il customerUuid da Vonage, controlla questo
   * set e termina immediatamente la chiamata cliente se presente.
   */
  private final java.util.Set<String> cancelledOperators =
      java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Inizializza il {@link VonageClient} con {@code applicationId}
   * e private key da configurazione.
   * {@code cti.vonage.application_id} e {@code cti.vonage.private_key}
   * sono obbligatori: se assenti viene lanciata {@link IllegalStateException}.
   * {@code cti.vonage.private_key} deve essere il percorso assoluto
   * del file {@code .key} sul filesystem.
   *
   * @param config configurazione applicazione (credenziali Vonage)
   */
  public VoiceHelper(Config config)
  {
    String appId;
    String keyPath;

    this.config = config;
    appId = config.get("cti.vonage.application_id", null);
    keyPath = config.get("cti.vonage.private_key", null);

    if (appId == null || appId.isBlank()) {
      throw new IllegalStateException("cti.vonage.application_id is required");
    }
    if (keyPath == null || keyPath.isBlank()) {
      throw new IllegalStateException("cti.vonage.private_key is required");
    }
    // Vonage SDK 9.x richiede che application_id sia un UUID valido
    try {
      UUID.fromString(appId);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "cti.vonage.application_id must be a valid UUID (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx). "
          + "Current value: \"" + appId + "\". "
          + "Get your Application ID from Vonage Dashboard: https://dashboard.nexmo.com/applications",
          e
      );
    }

    this.applicationId = appId;
    this.privateKeyPath = keyPath;
    vonageClient = VonageClient.builder()
        .applicationId(applicationId)
        .privateKeyPath(privateKeyPath)
        .build();
  }

  /**
   * Costruisce e serializza l'NCCO per l'operatore in attesa.
   * L'operatore entra nella conversazione con {@code startOnEnter: false} e
   * sente la musica di attesa. La conversazione si avvia quando il cliente entra.
   *
   * @param conversationName nome univoco della conversazione
   * @param musicOnHoldUrl   URL audio per la musica di attesa
   * @return stringa JSON del NCCO da restituire a Vonage
   *
   * TODO: verificare cosa viene ascoltato dall'operatore durante l'attesa
   *       in assenza di musica configurata
   */
  public String buildOperatorNccoJson(String conversationName,
                                      String musicOnHoldUrl)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;
    List<String> musicList;

    musicList = new ArrayList<>();
    musicList.add(musicOnHoldUrl);

    // azione NCCO di tipo conversation per l'operatore
    conversation = new HashMap<>();
    // tipo di azione NCCO
    conversation.put("action", "conversation");
    // nome condiviso tra la leg dell'operatore e quella del cliente
    conversation.put("name", conversationName);
    // l'operatore attende senza attivare la conversazione
    conversation.put("startOnEnter", false);
    // audio riprodotto all'operatore durante l'attesa del cliente
    conversation.put("musicOnHoldUrl", musicList);

    ncco = new ArrayList<>();
    ncco.add(conversation);

    return Json.encode(ncco);
  }

  /**
   * Chiama il cliente tramite il Vonage Java SDK.
   * Il cliente entra nella stessa conversazione dell'operatore con
   * {@code startOnEnter: true} (default), avviando la conversazione.
   *
   * <p>Registra la relazione operatorUuid → customerUuid nella mappa in-memory
   * e persiste il record nel database.</p>
   *
   * @param customerNumber   numero telefonico del cliente
   * @param conversationName nome della conversazione (condiviso con l'operatore)
   * @param operatorUuid     UUID della chiamata dell'operatore
   * @param db               connessione DB per persistenza
   */
  /**
   * Chiama il cliente tramite il Vonage Java SDK.
   * Il cliente entra nella stessa conversazione dell'operatore con
   * {@code startOnEnter: true} (default), avviando la conversazione.
   *
   * <p>Registra la relazione operatorUuid → customerUuid nella mappa in-memory
   * e persiste il record nel database.</p>
   *
   * @param customerNumber   numero telefonico del cliente
   * @param conversationName nome della conversazione (condiviso con l'operatore)
   * @param operatorUuid     UUID della chiamata dell'operatore
   * @param operatoreId      id locale dell'operatore in {@code jms_cti_operatori}, o null
   * @param chiamanteAccountId id account dell'operatore chiamante, o null
   * @param contattoId       id del contatto CRM di origine, o null
   * @param callbackUrl      URL da notificare per eventi di chiamata, o null
   * @param db               connessione DB per persistenza
   */
  public void callCustomer(String customerNumber, String conversationName,
                           String operatorUuid, Long operatoreId,
                           Long chiamanteAccountId, Long contattoId,
                           String callbackUrl, DB db)
                           throws Exception
  {
    String fromNumber;
    String answerUrl;
    String eventUrl;
    ConversationAction conversationAction;
    Call call;
    CallEvent callEvent;
    String customerUuid;
    String status;
    String direction;
    String conversationUuid;
    CallDAO dao;
    CallDTO dto;

    log.info("[CTI] callCustomer: to={}, conversation={}",
             customerNumber, conversationName);

    fromNumber = config.get("cti.vonage.from_number", "");
    answerUrl  = config.get("cti.vonage.answer_url", "");
    eventUrl   = config.get("cti.vonage.event_url", "");

    conversationAction = ConversationAction.builder(conversationName).build();
    call = new Call(customerNumber, fromNumber, List.of(conversationAction));

    callEvent = vonageClient.getVoiceClient().createCall(call);
    customerUuid = callEvent.getUuid();

    status = callEvent.getStatus() != null
      ? callEvent.getStatus().name()
      : null;

    direction = callEvent.getDirection() != null
      ? callEvent.getDirection().name()
      : null;

    conversationUuid = callEvent.getConversationUuid();

    log.info("[CTI] callCustomer risposta Vonage: uuid={}, status={}",
             customerUuid, status);

    if (cancelledOperators.remove(operatorUuid)) {
      log.info("[CTI] callCustomer: operatore già annullato, termino chiamata cliente uuid={}", customerUuid);
      vonageClient.getVoiceClient().terminateCall(customerUuid);
    } else {
      outgoingCalls.put(operatorUuid, customerUuid);
    }

    dto = new CallDTO(
        null, customerUuid, conversationUuid, direction, status,
        "phone", fromNumber,
        "phone", customerNumber,
        null, null, null, null, null, null,
        answerUrl, eventUrl,
        null, null, operatoreId, chiamanteAccountId, contattoId, callbackUrl, null, null);

    dao = new CallDAO(db);
    dao.insert(dto);
  }

  /**
   * Processa un evento Vonage Voice e aggiorna il record corrispondente
   * in {@code jms_chiamate}.
   *
   * <p>Gestisce i seguenti stati:
   * <ul>
   *   <li>{@code answered} — imposta {@code ora_inizio} e {@code stato}</li>
   *   <li>{@code completed} — imposta {@code ora_fine}, {@code durata},
   *       dati di billing ({@code tariffa}, {@code costo}, {@code rete})</li>
   *   <li>stati di errore — aggiorna solo {@code stato}</li>
   * </ul>
   * Gli eventi RTC privi di {@code uuid} o {@code status} vocale vengono ignorati.
   * </p>
   *
   * @param body corpo del webhook decodificato
   * @param db   connessione DB per l'aggiornamento
   */
  public void processEvent(HashMap<String, Object> body, DB db) throws Exception
  {
    String uuid;
    String status;
    String timestamp;
    String startTime;
    String endTime;
    String durationStr;
    String rate;
    String price;
    String network;
    String fromUser;
    CallDAO dao;
    CallDTO call;
    HashMap<String, Object> cbData;
    dev.jms.app.module.cti.vonage.dao.OperatorDAO opDao;
    dev.jms.app.module.cti.vonage.dto.OperatorDTO op;
    dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO sessioneDao;
    LocalDateTime oraInizio;
    LocalDateTime oraFine;
    Integer durata;

    uuid        = DB.toString(body.get("uuid"));
    status      = DB.toString(body.get("status"));
    timestamp   = DB.toString(body.get("timestamp"));
    startTime   = DB.toString(body.get("start_time"));
    endTime     = DB.toString(body.get("end_time"));
    durationStr = DB.toString(body.get("duration"));
    rate        = DB.toString(body.get("rate"));
    price       = DB.toString(body.get("price"));
    network     = DB.toString(body.get("network"));
    fromUser    = DB.toString(body.get("from_user"));

    if (uuid == null || uuid.isBlank() || status == null || status.isBlank()) {
      return;
    }

    dao = new CallDAO(db);

    if ("answered".equals(status)) {
      oraInizio = parseTimestamp(timestamp);
      dao.updateOnAnswer(uuid, oraInizio);
      call = dao.findByUuid(uuid);
      if (call != null && call.callbackUrl() != null && !call.callbackUrl().isBlank()) {
        fireCallback(call.callbackUrl(), call.contattoId(), "call_answered", new HashMap<>());
      }
      if (fromUser != null && !fromUser.isBlank()) {
        opDao = new dev.jms.app.module.cti.vonage.dao.OperatorDAO(db);
        op    = opDao.findByVonageUserId(fromUser);
        if (op != null) {
          sessioneDao = new dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO(db);
          sessioneDao.setInChiamata(op.id());
        }
      }
    } else if ("completed".equals(status)) {
      oraInizio = parseTimestamp(startTime != null && !startTime.isBlank() ? startTime : timestamp);
      oraFine   = parseTimestamp(endTime   != null && !endTime.isBlank()   ? endTime   : timestamp);
      durata    = null;
      if (durationStr != null && !durationStr.isBlank()) {
        try {
          durata = Integer.parseInt(durationStr.trim());
        } catch (NumberFormatException e) {
          log.warn("[CTI] processEvent: durata non parsabile: {}", durationStr);
        }
      }
      dao.updateOnComplete(uuid, oraInizio, oraFine, durata, rate, price, network);
      call = dao.findByUuid(uuid);
      if (call != null && call.callbackUrl() != null && !call.callbackUrl().isBlank()) {
        cbData = new HashMap<>();
        cbData.put("duration", durata);
        fireCallback(call.callbackUrl(), call.contattoId(), "call_ended", cbData);
      }
      if (fromUser != null && !fromUser.isBlank()) {
        opDao = new dev.jms.app.module.cti.vonage.dao.OperatorDAO(db);
        op    = opDao.findByVonageUserId(fromUser);
        if (op != null) {
          sessioneDao = new dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO(db);
          sessioneDao.registraFineChiamata(op.id(), durata != null ? durata : 0);
        }
      }
    } else if ("failed".equals(status)    || "rejected".equals(status)
            || "busy".equals(status)      || "timeout".equals(status)
            || "cancelled".equals(status) || "unanswered".equals(status)
            || "machine".equals(status)) {
      dao.updateStatus(uuid, status);
    }
  }

  /**
   * Invia una notifica HTTP POST all'endpoint di callback in modo asincrono (fire-and-forget).
   * Il body è {@code {"id": contattoId, "type": type, "data": data}}.
   * Gli errori vengono loggati ma non propagati.
   *
   * @param callbackUrl URL del destinatario della notifica
   * @param contattoId  id del contatto CRM, inoltrato come-is al destinatario
   * @param type        tipo di evento (es. {@code call_answered}, {@code call_ended})
   * @param data        dati aggiuntivi specifici dell'evento
   */
  private void fireCallback(String callbackUrl, Long contattoId,
                            String type, HashMap<String, Object> data)
  {
    Thread t;
    String body;
    HashMap<String, Object> payload;

    payload = new HashMap<>();
    payload.put("id", contattoId);
    payload.put("type", type);
    payload.put("data", data);
    body = Json.encode(payload);

    t = new Thread(() -> {
      HttpClient client;
      java.net.http.HttpRequest httpReq;

      try {
        client = HttpClient.newHttpClient();
        httpReq = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(callbackUrl))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
            .build();
        client.send(httpReq, HttpResponse.BodyHandlers.discarding());
        log.info("[CTI] fireCallback: url={}, type={}, contattoId={}", callbackUrl, type, contattoId);
      } catch (Exception e) {
        log.warn("[CTI] fireCallback: errore notifica url={}: {}", callbackUrl, e.getMessage());
      }
    });
    t.setDaemon(true);
    t.start();
  }

  /**
   * Converte una stringa timestamp ISO 8601 in {@link LocalDateTime} UTC.
   * Restituisce {@code null} se la stringa è null, vuota o non parsabile.
   *
   * @param ts timestamp in formato ISO 8601 (es. {@code 2026-01-15T10:30:00.000Z})
   * @return LocalDateTime UTC corrispondente, o {@code null}
   */
  private LocalDateTime parseTimestamp(String ts)
  {
    LocalDateTime result;

    result = null;
    if (ts != null && !ts.isBlank()) {
      try {
        result = Instant.parse(ts).atZone(ZoneOffset.UTC).toLocalDateTime();
      } catch (Exception e) {
        log.warn("[CTI] parseTimestamp: impossibile parsare '{}': {}", ts, e.getMessage());
      }
    }
    return result;
  }

  /**
   * Riagancia la chiamata dell'operatore e, se presente, quella del cliente.
   * Chiama {@code terminateCall()} su entrambe le legs
   * tramite il Vonage Java SDK.
   *
   * @param operatorUuid UUID della chiamata dell'operatore
   */
  public void hangupCall(String operatorUuid) throws Exception
  {
    String customerUuid;

    log.info("[CTI] hangupCall: operatorUuid={}", operatorUuid);

    vonageClient.getVoiceClient().terminateCall(operatorUuid);

    customerUuid = outgoingCalls.remove(operatorUuid);
    if (customerUuid != null) {
      vonageClient.getVoiceClient().terminateCall(customerUuid);
    } else {
      cancelledOperators.add(operatorUuid);
      log.warn("[CTI] hangupCall: chiamata cliente non ancora avviata per operatore={}, annullamento registrato",
               operatorUuid);
    }
  }

  /**
   * Crea un utente Vonage tramite {@code UsersClient} e lo registra nell'applicazione.
   * Il {@code name} diventa il claim {@code sub} del JWT SDK e corrisponde
   * al campo {@code vonage_user_id} in {@code cti_operatori}.
   *
   * @param name        nome univoco dell'utente Vonage (es. {@code operatore_01})
   * @param displayName nome visualizzato, o {@code null}
   * @return nome dell'utente creato (identico a {@code name} se accettato da Vonage)
   * @throws UsersResponseException se Vonage rifiuta la creazione (es. nome duplicato)
   */
  public String createVonageUser(String name, String displayName) throws UsersResponseException
  {
    User.Builder builder;
    User user;
    User created;

    builder = User.builder();
    builder.name(name);
    if (displayName != null && !displayName.isBlank()) {
      builder.displayName(displayName);
    }
    user = builder.build();
    created = vonageClient.getUsersClient().createUser(user);
    log.info("[CTI] createVonageUser: name={}, vonageId={}", name, created.getId());
    return created.getName();
  }

  /**
   * Restituisce tutti gli utenti registrati sull'applicazione Vonage.
   * Ogni elemento contiene {@code vonageId} (ID interno USR-xxx),
   * {@code name} (il campo {@code vonage_user_id} locale) e {@code displayName}.
   *
   * @return lista di utenti Vonage
   */
  public List<HashMap<String, Object>> listVonageUsers()
  {
    List<BaseUser> users;
    List<HashMap<String, Object>> result;
    HashMap<String, Object> entry;

    result = new ArrayList<>();
    users = vonageClient.getUsersClient().listUsers();
    if (users == null) {
      return result;
    }
    for (BaseUser user : users) {
      entry = new HashMap<>();
      entry.put("vonageId", user.getId());
      entry.put("name", user.getName());
      entry.put("displayName", user instanceof User ? ((User) user).getDisplayName() : null);
      result.add(entry);
    }
    return result;
  }

  /**
   * Elimina un utente Vonage cercandolo per nome.
   * Effettua una listUsers per recuperare l'ID interno (USR-xxx) necessario
   * alla chiamata deleteUser.
   *
   * @param name nome dell'utente Vonage (corrisponde a {@code vonage_user_id} locale)
   */
  public void deleteVonageUser(String name)
  {
    List<BaseUser> users;
    String vonageId;

    vonageId = null;
    users = vonageClient.getUsersClient().listUsers();
    if (users != null) {
      for (BaseUser user : users) {
        if (name.equals(user.getName())) {
          vonageId = user.getId();
          break;
        }
      }
    }
    if (vonageId != null) {
      vonageClient.getUsersClient().deleteUser(vonageId);
      log.info("[CTI] deleteVonageUser: name={}, vonageId={}", name, vonageId);
    } else {
      log.warn("[CTI] deleteVonageUser: utente non trovato su Vonage: {}", name);
    }
  }

  public String generateSdkJwt(String userId) throws Exception
  {
    HashMap<String, Object> aclPaths;
    HashMap<String, Object> acl;

    // percorsi API Vonage a cui l'operatore è autorizzato tramite JWT
    aclPaths = new HashMap<>();
    aclPaths.put("/*/users/**", new HashMap<>());
    aclPaths.put("/*/conversations/**", new HashMap<>());
    aclPaths.put("/*/sessions/**", new HashMap<>());
    aclPaths.put("/*/devices/**", new HashMap<>());
    aclPaths.put("/*/image/**", new HashMap<>());
    aclPaths.put("/*/media/**", new HashMap<>());
    aclPaths.put("/*/push/**", new HashMap<>());
    aclPaths.put("/*/knocking/**", new HashMap<>());
    aclPaths.put("/*/legs/**", new HashMap<>());

    // claim acl richiesto dal Vonage Client SDK per la connessione WebRTC
    acl = new HashMap<>();
    acl.put("paths", aclPaths);

    return Jwt.builder()
        .applicationId(applicationId)
        .privateKeyPath(Paths.get(privateKeyPath))
        .subject(userId)
        .expiresAt(ZonedDateTime.now().plusSeconds(3600))
        .addClaim("acl", acl)
        .build()
        .generate();
  }
}
