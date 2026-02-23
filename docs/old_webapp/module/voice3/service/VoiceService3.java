package dev.crm.module.voice3.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.crm.module.cloud.dto.InstallationMetadataDto;
import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.voice3.config.Voice3Config;
import dev.crm.module.voice3.dao.CallDao3;
import dev.crm.module.voice3.dto.CallDto3;
import dev.springtools.util.JSON;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service principale del modulo voice3.
 *
 * Implementa il pattern "operator-first progressive dialer":
 *
 * FLUSSO COMPLETO:
 *   1. Il frontend chiama POST /api/voice3/prepare-call con userId e customerNumber.
 *      Il backend salva in memoria la coppia userId → customerNumber.
 *
 *   2. Il frontend esegue client.serverCall() tramite il Vonage Client SDK.
 *      Vonage contatta il backend sull'answer_url configurato nel Dashboard.
 *
 *   3. Il backend riceve il webhook POST /api/voice3/answer.
 *      Recupera il customerNumber dalla mappa in-memory tramite userId (from_user).
 *      Risponde a Vonage con un NCCO che fa entrare l'operatore in una conversazione
 *      con musica di attesa (startOnEnter: false → l'operatore non sente nessuno ancora).
 *
 *   4. In modo asincrono (thread separato, dopo 1 secondo), il backend chiama
 *      il cliente tramite POST all'API Vonage /v1/calls con un NCCO che fa
 *      entrare il cliente nella stessa conversazione (startOnEnter: true → avvia la conv).
 *
 *   5. Quando il cliente risponde ed entra nella conversazione, l'audio si sblocca
 *      e l'operatore inizia a sentire il cliente.
 *
 *   6. Per riagganciare: il frontend chiama PUT /api/voice3/calls/{uuid}/hangup.
 *      Il backend riagancia sia la chiamata dell'operatore che quella del cliente.
 */
public class VoiceService3
{
  private static final Logger log = LoggerFactory.getLogger(VoiceService3.class);

  private final Voice3Config config;
  private final CallDao3 callDao;
  private final InstallationService installationService;

  /**
   * Mappa in-memory delle chiamate preparate: userId → customerNumber.
   * Viene popolata da prepareCall() e consumata (con rimozione) da getPendingCustomerNumber().
   * ConcurrentHashMap per gestione thread-safe (il webhook arriva su thread separato).
   * Assunzione: un operatore può avere al massimo una chiamata pendente alla volta.
   */
  private final Map<String, String> pendingCalls = new ConcurrentHashMap<>();

  /**
   * Mappa in-memory che associa la chiamata operatore alla chiamata cliente.
   * operatorCallUuid → customerCallUuid
   * Necessaria per riagganciare entrambe le chiamate con un singolo hangup.
   */
  private final Map<String, String> operatorToCustomerCalls = new ConcurrentHashMap<>();

  public VoiceService3(
      Voice3Config config,
      CallDao3 callDao,
      InstallationService installationService)
  {
    this.config = config;
    this.callDao = callDao;
    this.installationService = installationService;
  }

  /**
   * Fase 1 del flusso: il frontend registra il numero del cliente da chiamare.
   * Viene invocato prima di serverCall() per garantire che il backend
   * abbia il customerNumber disponibile quando arriva il webhook answer.
   *
   * @param userId       identificatore dell'operatore nel sistema Vonage
   * @param customerNumber numero telefonico del cliente da chiamare
   */
  public void prepareCall(String userId, String customerNumber)
  {
    log.info("[PREPARE_CALL] userId={}, customerNumber={}", userId, customerNumber);
    pendingCalls.put(userId, customerNumber);
    log.info("[PREPARE_CALL] chiamate pendenti in memoria: {}", pendingCalls.size());
  }

  /**
   * Recupera e rimuove il numero del cliente associato all'operatore.
   * La rimozione è atomica: dopo questo punto il numero non è più disponibile,
   * impedendo che lo stesso numero venga usato per due chiamate.
   *
   * @param userId identificatore dell'operatore
   * @return numero del cliente, oppure null se non trovato
   */
  public String getPendingCustomerNumber(String userId)
  {
    String customerNumber;

    customerNumber = pendingCalls.remove(userId);
    log.info("[GET_PENDING] userId={}, customerNumber={}", userId, customerNumber);

    return customerNumber;
  }

  /**
   * Costruisce l'NCCO per l'operatore in attesa del cliente.
   * L'operatore entra nella conversazione con startOnEnter: false,
   * il che significa che:
   *   - sente la musica di attesa
   *   - NON viene segnalato come entrato nella conversazione
   *   - la conversazione parte solo quando entra il cliente (startOnEnter: true)
   *
   * @param conversationName nome univoco della conversazione (generato localmente)
   * @param musicOnHoldUrl   URL audio MP3 per la musica di attesa
   */
  public List<Map<String, Object>> buildOperatorWaitingNcco(
      String conversationName,
      String musicOnHoldUrl)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;

    ncco = new ArrayList<>();
    conversation = new HashMap<>();
    conversation.put("action", "conversation");
    conversation.put("name", conversationName);
    // L'operatore entra in modalità silenziosa: sente la musica, non parla
    conversation.put("startOnEnter", false);
    conversation.put("musicOnHoldUrl", List.of(musicOnHoldUrl));

    ncco.add(conversation);

    return ncco;
  }

  /**
   * Costruisce l'NCCO per il cliente che entra nella conversazione.
   * Con startOnEnter: true (default), l'ingresso del cliente avvia la conversazione
   * e sblocca l'audio bidirezionale con l'operatore.
   *
   * @param conversationName nome della conversazione a cui unirsi (stessa dell'operatore)
   */
  public List<Map<String, Object>> buildCustomerJoinNcco(String conversationName)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;

    ncco = new ArrayList<>();
    conversation = new HashMap<>();
    conversation.put("action", "conversation");
    conversation.put("name", conversationName);
    // Il cliente entra normalmente: la sua entrata avvia la conversazione

    ncco.add(conversation);

    return ncco;
  }

  /**
   * Fase 4 del flusso: chiama il cliente tramite le Vonage Voice API.
   * Viene eseguito in modo asincrono dal controller dopo aver risposto al webhook answer.
   *
   * Costruisce l'event_url con installation_id, conversation_uuid e token HMAC
   * per autenticare i webhook in arrivo da Vonage (vedi docs/webhook-auth.md).
   *
   * Al completamento della chiamata, registra il record nel DB e salva la
   * relazione operatore → cliente per il successivo hangup.
   *
   * @param customerNumber   numero telefonico del cliente
   * @param conversationName nome della conversazione (condiviso con l'operatore)
   * @param operatorCallUuid UUID della chiamata dell'operatore (per la mappa hangup)
   */
  public void callCustomer(
      String customerNumber,
      String conversationName,
      String operatorCallUuid) throws Exception
  {
    Map<String, Object> payload;
    String vonageToken;
    String responseBody;
    Map<String, Object> responseMap;
    String uuid;
    String status;
    String direction;
    String conversationUuid;
    CallDto3 callDto;
    List<Map<String, Object>> ncco;
    String eventUrl;
    String installationToken;
    String installationId;
    InstallationMetadataDto metadata;

    log.info("[CALL_CUSTOMER] to={}, conversation={}", customerNumber, conversationName);

    // Recupera o crea il record di installazione per costruire l'event_url autenticato
    metadata = installationService.getOrCreateInstallation();
    installationId = metadata.installationId;

    // Genera il token HMAC che Vonage rimanderà in ogni webhook per autenticarsi
    // Il conversationName viene incluso nel token come conversation_uuid
    installationToken = installationService.generateEventUrlToken(conversationName);

    // Costruisce l'event_url con i parametri di autenticazione:
    //   installation_id: identifica questa installazione
    //   conversation_uuid: identifica la conversazione (per routing nel service cloud)
    //   token: firma HMAC per verificare l'autenticità del webhook
    eventUrl = config.getEventUrl()
      + "?installation_id=" + installationId
      + "&conversation_uuid=" + conversationName
      + "&token=" + installationToken;

    log.info("[EVENT_URL] installation_id={}, url={}", installationId, eventUrl);

    ncco = buildCustomerJoinNcco(conversationName);

    // Costruisce il payload per le Vonage Voice API
    payload = new HashMap<>();
    payload.put("to", List.of(Map.of("type", "phone", "number", customerNumber)));
    payload.put("from", Map.of("type", "phone", "number", config.getFromNumber()));
    payload.put("ncco", ncco);
    payload.put("event_url", List.of(eventUrl));

    log.debug("[CALL_CUSTOMER] payload={}", payload);

    // Ottiene il token JWT per autenticarsi con le API Vonage
    vonageToken = resolveToken();

    log.info("[CALL_CUSTOMER] invio richiesta a Vonage...");
    responseBody = postJson(config.getBaseUrl(), payload, vonageToken);

    // Vonage risponde con uuid, status, direction, conversation_uuid della chiamata creata
    responseMap = (Map<String, Object>) JSON.decode(responseBody, Map.class);

    uuid = (String) responseMap.get("uuid");
    status = (String) responseMap.get("status");
    direction = (String) responseMap.get("direction");
    conversationUuid = (String) responseMap.get("conversation_uuid");

    log.info("[CALL_CREATED] uuid={}, conversation_uuid={}, status={}", uuid, conversationUuid, status);

    // Persiste la chiamata nel DB per tracciabilità
    callDto = new CallDto3();
    callDto.uuid = uuid;
    callDto.conversationUuid = conversationUuid;
    callDto.status = status;
    callDto.direction = direction;
    callDto.fromType = "phone";
    callDto.fromNumber = config.getFromNumber();
    callDto.toType = "phone";
    callDto.toNumber = customerNumber;
    callDto.eventUrl = eventUrl;
    callDto.createdAt = LocalDateTime.now();

    callDao.insert(callDto);

    // Registra la relazione tra chiamata operatore e chiamata cliente
    // necessaria per il hangup simultaneo di entrambe le chiamate
    operatorToCustomerCalls.put(operatorCallUuid, uuid);
    log.info("[CALL_CUSTOMER] relazione salvata: operator={} -> customer={}", operatorCallUuid, uuid);
  }

  /**
   * Riagancia la chiamata dell'operatore e, se presente, anche quella del cliente.
   * Recupera il customerCallUuid dalla mappa in-memory e invia PUT /v1/calls/{uuid}
   * con action=hangup per entrambe le chiamate.
   *
   * @param operatorCallUuid UUID della chiamata dell'operatore da riagganciare
   */
  public void hangupCall(String operatorCallUuid) throws Exception
  {
    String url;
    String vonageToken;
    Map<String, Object> payload;
    String customerCallUuid;

    log.info("[HANGUP] operatorCallUuid={}", operatorCallUuid);

    vonageToken = resolveToken();
    payload = new HashMap<>();
    payload.put("action", "hangup");

    // Riagancia la chiamata dell'operatore
    url = config.getBaseUrl() + "/" + operatorCallUuid;
    log.info("[HANGUP] riagancio operatore: {}", operatorCallUuid);
    putJson(url, payload, vonageToken);

    // Cerca e riagancia l'eventuale chiamata associata del cliente
    customerCallUuid = operatorToCustomerCalls.remove(operatorCallUuid);
    if (customerCallUuid != null) {
      log.info("[HANGUP] trovata chiamata cliente associata: {}", customerCallUuid);
      url = config.getBaseUrl() + "/" + customerCallUuid;
      putJson(url, payload, vonageToken);
    } else {
      log.warn("[HANGUP] nessuna chiamata cliente trovata per operatore: {}", operatorCallUuid);
    }

    log.info("[HANGUP] completato");
  }

  /**
   * Genera il token JWT per autenticarsi con le Vonage Voice API.
   * Se in configurazione è presente un token statico (voice.token), lo usa direttamente.
   * Altrimenti genera un JWT RS256 dinamico firmato con la private.key Vonage
   * con scadenza di 5 minuti.
   */
  private String resolveToken() throws Exception
  {
    String token;

    token = config.getToken();

    if (token == null || token.isEmpty()) {
      token = generateVonageJwt();
    }

    return token;
  }

  /**
   * Genera un JWT RS256 per autenticarsi con le Vonage Voice API.
   * Utilizza la libreria auth0/java-jwt per la firma.
   * La chiave privata viene letta dal path configurato in voice.private-key.
   *
   * Claims obbligatori per Vonage:
   *   - application_id: identifica l'app Vonage
   *   - iat: issued at (timestamp emissione)
   *   - exp: expiry (5 minuti)
   *   - jti: JWT ID univoco (previene replay attack)
   */
  private String generateVonageJwt() throws Exception
  {
    String privateKeyPath;
    String privateKeyPem;
    RSAPrivateKey privateKey;
    Algorithm algorithm;
    String jwt;

    privateKeyPath = config.getPrivateKey();
    privateKeyPem = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
    privateKeyPem = privateKeyPem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    privateKey = (RSAPrivateKey) kf.generatePrivate(spec);

    algorithm = Algorithm.RSA256(null, privateKey);

    jwt = JWT
        .create()
        .withClaim("application_id", config.getApplicationId())
        .withIssuedAt(Instant.now())
        .withExpiresAt(Instant.now().plusSeconds(300))
        .withJWTId(UUID.randomUUID().toString())
        .sign(algorithm);

    return jwt;
  }

  /**
   * Genera il JWT SDK per autenticare l'operatore nel Vonage Client SDK lato browser.
   * A differenza del JWT API (RS256 firmato con auth0), questo viene costruito
   * manualmente per includere i claims ACL specifici per il Client SDK.
   *
   * Claims SDK:
   *   - application_id: app Vonage
   *   - sub: userId dell'operatore (identità nel sistema Vonage)
   *   - acl/paths: permessi sulle risorse Vonage Conversations API
   *   - exp: scadenza 1 ora
   *
   * @param userId identificatore dell'operatore nel sistema Vonage
   * @return token JWT firmato RS256, da passare a VonageClient.createSession()
   */
  public String generateSdkJwt(String userId) throws Exception
  {
    PrivateKey privateKey;
    long now;
    Map<String, Object> aclPaths;
    Map<String, Object> claims;
    String header;
    String payload;
    Signature signer;
    byte[] signatureBytes;
    String signature;
    String token;

    privateKey = loadPrivateKey();
    now = Instant.now().getEpochSecond();

    // Permessi ACL per il Vonage Client SDK: accesso a tutte le risorse necessarie
    aclPaths = new HashMap<>();
    aclPaths.put("/*/users/**", Map.of());
    aclPaths.put("/*/conversations/**", Map.of());
    aclPaths.put("/*/sessions/**", Map.of());
    aclPaths.put("/*/devices/**", Map.of());
    aclPaths.put("/*/image/**", Map.of());
    aclPaths.put("/*/media/**", Map.of());
    aclPaths.put("/*/push/**", Map.of());
    aclPaths.put("/*/knocking/**", Map.of());
    aclPaths.put("/*/legs/**", Map.of());

    claims = new HashMap<>();
    claims.put("application_id", config.getApplicationId());
    claims.put("iat", now);
    claims.put("exp", now + 3600);  // scadenza 1 ora
    claims.put("jti", UUID.randomUUID().toString());
    claims.put("sub", userId);      // identità dell'operatore
    claims.put("acl", Map.of("paths", aclPaths));

    // Costruisce manualmente il JWT: header.payload.signature in base64url
    header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(JSON.encode(claims).getBytes(StandardCharsets.UTF_8));

    signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
    signatureBytes = signer.sign();

    signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

    token = header + "." + payload + "." + signature;

    return token;
  }

  /**
   * Carica la chiave privata RSA dalla configurazione.
   * Supporta due formati:
   *   - path al file .key (se non contiene "BEGIN PRIVATE KEY")
   *   - contenuto PEM inline
   */
  private PrivateKey loadPrivateKey() throws Exception
  {
    String configValue;
    String pemContent;
    String base64Key;
    byte[] derBytes;
    KeyFactory keyFactory;
    PrivateKey privateKey;

    configValue = config.getPrivateKey();

    if (!configValue.contains("BEGIN PRIVATE KEY")) {
      // È un path: legge il file
      pemContent = Files.readString(Paths.get(configValue));
    } else {
      // È il contenuto PEM inline
      pemContent = configValue;
    }

    base64Key = pemContent
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");

    derBytes = Base64.getDecoder().decode(base64Key);
    keyFactory = KeyFactory.getInstance("RSA");
    privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(derBytes));

    return privateKey;
  }

  /**
   * Esegue una richiesta HTTP POST con body JSON verso l'API Vonage.
   * Usato per creare nuove chiamate (POST /v1/calls).
   *
   * @param url     URL completo dell'endpoint
   * @param payload corpo della richiesta come Map (serializzato in JSON)
   * @param token   JWT Bearer per autenticazione
   * @return corpo della risposta come stringa JSON
   */
  private String postJson(String url, Map<String, Object> payload, String token) throws Exception
  {
    URL targetUrl;
    HttpURLConnection conn;
    String body;
    byte[] input;
    String responseBody;

    body = JSON.encode(payload);

    targetUrl = new URL(url);
    conn = (HttpURLConnection) targetUrl.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + token);
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      input = body.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    try (Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
      responseBody = sc.useDelimiter("\\A").next();
    }

    return responseBody;
  }

  /**
   * Esegue una richiesta HTTP PUT con body JSON verso l'API Vonage.
   * Usato per modificare lo stato di una chiamata esistente (es. hangup).
   *
   * @param url     URL completo dell'endpoint (es. /v1/calls/{uuid})
   * @param payload corpo della richiesta (es. {"action": "hangup"})
   * @param token   JWT Bearer per autenticazione
   */
  private void putJson(String url, Map<String, Object> payload, String token) throws Exception
  {
    URL targetUrl;
    HttpURLConnection conn;
    String body;
    byte[] input;

    body = JSON.encode(payload);

    targetUrl = new URL(url);
    conn = (HttpURLConnection) targetUrl.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + token);
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      input = body.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    try (Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
      sc.useDelimiter("\\A").hasNext();
    }
  }
}
