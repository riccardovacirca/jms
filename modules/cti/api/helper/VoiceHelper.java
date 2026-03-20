package dev.jms.app.cti.helper;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.jms.app.cti.dao.CallDAO;
import dev.jms.app.cti.dto.CallDTO;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Json;
import dev.jms.util.Log;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic del modulo CTI: gestione del flusso operator-first progressive dialer.
 *
 * <p>Mantiene una mappa in-memory thread-safe:</p>
 * <ul>
 *   <li>{@code operatorToCustomerCalls}: operatorUuid → customerUuid (per hangup simultaneo)</li>
 * </ul>
 *
 * <p>Flusso completo:</p>
 * <ol>
 *   <li>Frontend esegue {@code client.serverCall({ customerNumber })} via Vonage Client SDK</li>
 *   <li>Vonage chiama {@code POST /api/cti/answer?customerNumber=...} → handler risponde con NCCO operatore</li>
 *   <li>Dopo 1s: {@link #callCustomer} chiama il cliente tramite Vonage API</li>
 *   <li>Per riagganciare: {@link #hangupCall} termina entrambe le chiamate</li>
 * </ol>
 */
public class VoiceHelper
{
  private static final Log log = Log.get(VoiceHelper.class);

  private final Config config;

  /**
   * Mapping operatore → cliente per il hangup simultaneo: operatorUuid → customerUuid.
   */
  private final Map<String, String> operatorToCustomerCalls = new ConcurrentHashMap<>();

  /**
   * @param config configurazione applicazione (credenziali Vonage)
   */
  public VoiceHelper(Config config)
  {
    this.config = config;
  }

  /**
   * Costruisce e serializza l'NCCO per l'operatore in attesa.
   * L'operatore entra nella conversazione con {@code startOnEnter: false} e
   * sente la musica di attesa. La conversazione si avvia quando il cliente entra.
   *
   * @param conversationName nome univoco della conversazione
   * @param musicOnHoldUrl   URL audio per la musica di attesa
   * @return stringa JSON del NCCO da restituire a Vonage
   */
  public String buildOperatorNccoJson(String conversationName, String musicOnHoldUrl)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;
    List<String> musicList;

    musicList = new ArrayList<>();
    musicList.add(musicOnHoldUrl);

    conversation = new HashMap<>();
    conversation.put("action", "conversation");
    conversation.put("name", conversationName);
    conversation.put("startOnEnter", false);
    conversation.put("musicOnHoldUrl", musicList);

    ncco = new ArrayList<>();
    ncco.add(conversation);

    return Json.encode(ncco);
  }

  /**
   * Chiama il cliente tramite le Vonage Voice API.
   * Il cliente entra nella stessa conversazione dell'operatore con
   * {@code startOnEnter: true} (default), avviando la conversazione bidirezionale.
   *
   * <p>Registra la relazione operatorUuid → customerUuid nella mappa in-memory
   * e persiste il record nel database.</p>
   *
   * @param customerNumber   numero telefonico del cliente
   * @param conversationName nome della conversazione (condiviso con l'operatore)
   * @param operatorUuid     UUID della chiamata dell'operatore
   * @param db               connessione DB per persistenza
   */
  @SuppressWarnings("unchecked")
  public void callCustomer(
      String customerNumber,
      String conversationName,
      String operatorUuid,
      DB db) throws Exception
  {
    Map<String, Object> nccoAction;
    List<Map<String, Object>> ncco;
    Map<String, Object> toEntry;
    Map<String, Object> fromEntry;
    Map<String, Object> payload;
    String vonageToken;
    String responseBody;
    HashMap<String, Object> responseMap;
    String customerUuid;
    String status;
    String direction;
    String conversationUuid;
    CallDAO dao;
    CallDTO dto;
    String fromNumber;
    String baseUrl;
    String eventUrl;
    List<String> eventUrlList;

    log.info("[CTI] callCustomer: to={}, conversation={}", customerNumber, conversationName);

    fromNumber = config.get("cti.vonage.from-number", "");
    baseUrl    = config.get("cti.vonage.base-url", "");
    eventUrl   = config.get("cti.vonage.event-url", "");

    // NCCO cliente: entra nella conversazione (startOnEnter: true per default)
    nccoAction = new HashMap<>();
    nccoAction.put("action", "conversation");
    nccoAction.put("name", conversationName);
    ncco = new ArrayList<>();
    ncco.add(nccoAction);

    toEntry = new HashMap<>();
    toEntry.put("type", "phone");
    toEntry.put("number", customerNumber);

    fromEntry = new HashMap<>();
    fromEntry.put("type", "phone");
    fromEntry.put("number", fromNumber);

    eventUrlList = new ArrayList<>();
    eventUrlList.add(eventUrl);

    payload = new HashMap<>();
    payload.put("to", List.of(toEntry));
    payload.put("from", fromEntry);
    payload.put("ncco", ncco);
    payload.put("event_url", eventUrlList);

    vonageToken  = generateVonageJwt();
    responseBody = postJson(baseUrl, payload, vonageToken);
    responseMap  = Json.decode(responseBody, HashMap.class);

    customerUuid     = DB.toString(responseMap.get("uuid"));
    status           = DB.toString(responseMap.get("status"));
    direction        = DB.toString(responseMap.get("direction"));
    conversationUuid = DB.toString(responseMap.get("conversation_uuid"));

    log.info("[CTI] callCustomer risposta Vonage: uuid={}, status={}", customerUuid, status);

    // Traccia operatore → cliente per il hangup
    operatorToCustomerCalls.put(operatorUuid, customerUuid);

    // Persiste la chiamata nel DB
    dto = new CallDTO(
        null, customerUuid, conversationUuid, direction, status,
        "phone", fromNumber,
        "phone", customerNumber,
        null, null, null, null, null, null,
        null, eventUrl,
        null, null, null, null, null, null);
    dao = new CallDAO(db);
    dao.insert(dto);
  }

  /**
   * Riagancia la chiamata dell'operatore e, se presente, quella del cliente.
   * Invia {@code action: hangup} a entrambe le chiamate tramite le Vonage Voice API.
   *
   * @param operatorUuid UUID della chiamata dell'operatore
   */
  public void hangupCall(String operatorUuid) throws Exception
  {
    String vonageToken;
    Map<String, Object> payload;
    String url;
    String customerUuid;
    String baseUrl;

    log.info("[CTI] hangupCall: operatorUuid={}", operatorUuid);

    baseUrl      = config.get("cti.vonage.base-url", "");
    vonageToken  = generateVonageJwt();
    payload      = new HashMap<>();
    payload.put("action", "hangup");

    url = baseUrl + "/" + operatorUuid;
    putJson(url, payload, vonageToken);

    customerUuid = operatorToCustomerCalls.remove(operatorUuid);
    if (customerUuid != null) {
      url = baseUrl + "/" + customerUuid;
      putJson(url, payload, vonageToken);
    } else {
      log.warn("[CTI] hangupCall: nessuna chiamata cliente trovata per operatore={}", operatorUuid);
    }
  }

  /**
   * Genera il JWT SDK per autenticare l'operatore nel Vonage Client SDK lato browser.
   * Firmato RS256 con la private key Vonage; scadenza 1 ora.
   * Include i claims ACL necessari per le Vonage Conversations API.
   *
   * @param userId identificatore dell'operatore nel sistema Vonage
   * @return token JWT da passare a {@code VonageClient.createSession()}
   */
  public String generateSdkJwt(String userId) throws Exception
  {
    PrivateKey privateKey;
    long now;
    HashMap<String, Object> aclPaths;
    HashMap<String, Object> acl;
    HashMap<String, Object> claims;
    String header;
    String payload;
    Signature signer;
    byte[] signatureBytes;
    String signature;
    String token;

    privateKey = loadPrivateKey();
    now        = Instant.now().getEpochSecond();

    aclPaths = new HashMap<>();
    aclPaths.put("/*/users/**",         new HashMap<>());
    aclPaths.put("/*/conversations/**", new HashMap<>());
    aclPaths.put("/*/sessions/**",      new HashMap<>());
    aclPaths.put("/*/devices/**",       new HashMap<>());
    aclPaths.put("/*/image/**",         new HashMap<>());
    aclPaths.put("/*/media/**",         new HashMap<>());
    aclPaths.put("/*/push/**",          new HashMap<>());
    aclPaths.put("/*/knocking/**",      new HashMap<>());
    aclPaths.put("/*/legs/**",          new HashMap<>());

    acl = new HashMap<>();
    acl.put("paths", aclPaths);

    claims = new HashMap<>();
    claims.put("application_id", config.get("cti.vonage.application-id", ""));
    claims.put("iat", now);
    claims.put("exp", now + 3600);
    claims.put("jti", UUID.randomUUID().toString());
    claims.put("sub", userId);
    claims.put("acl", acl);

    header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(Json.encode(claims).getBytes(StandardCharsets.UTF_8));

    signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
    signatureBytes = signer.sign();

    signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    token     = header + "." + payload + "." + signature;

    return token;
  }

  /**
   * Genera il JWT RS256 per autenticarsi con le Vonage Voice API.
   * Claims obbligatori: {@code application_id}, {@code iat}, {@code exp} (5 min), {@code jti}.
   */
  private String generateVonageJwt() throws Exception
  {
    RSAPrivateKey privateKey;
    Algorithm algorithm;
    String jwt;

    privateKey = (RSAPrivateKey) loadPrivateKey();
    algorithm  = Algorithm.RSA256(null, privateKey);

    jwt = JWT.create()
        .withClaim("application_id", config.get("cti.vonage.application-id", ""))
        .withIssuedAt(Instant.now())
        .withExpiresAt(Instant.now().plusSeconds(300))
        .withJWTId(UUID.randomUUID().toString())
        .sign(algorithm);

    return jwt;
  }

  /**
   * Carica la chiave privata RSA dalla configurazione.
   * Supporta due formati per il valore di {@code cti.vonage.private-key}:
   * <ul>
   *   <li>Path al file {@code .key} (se il valore non contiene {@code BEGIN PRIVATE KEY})</li>
   *   <li>Contenuto PEM inline</li>
   * </ul>
   */
  private PrivateKey loadPrivateKey() throws Exception
  {
    String configValue;
    String pemContent;
    String base64Key;
    byte[] derBytes;
    KeyFactory keyFactory;
    PrivateKey privateKey;

    configValue = config.get("cti.vonage.private-key", "");

    if (!configValue.contains("BEGIN PRIVATE KEY")) {
      pemContent = Files.readString(Paths.get(configValue));
    } else {
      pemContent = configValue;
    }

    base64Key = pemContent
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");

    derBytes   = Base64.getDecoder().decode(base64Key);
    keyFactory = KeyFactory.getInstance("RSA");
    privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(derBytes));

    return privateKey;
  }

  /**
   * Esegue una richiesta HTTP POST con body JSON verso l'API Vonage.
   *
   * @param url     URL completo dell'endpoint
   * @param payload corpo della richiesta come Map
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

    body      = Json.encode(payload);
    targetUrl = new URL(url);
    conn      = (HttpURLConnection) targetUrl.openConnection();
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
   * Usato per modificare lo stato di una chiamata (es. hangup).
   *
   * @param url     URL completo dell'endpoint
   * @param payload corpo della richiesta
   * @param token   JWT Bearer per autenticazione
   */
  private void putJson(String url, Map<String, Object> payload, String token) throws Exception
  {
    URL targetUrl;
    HttpURLConnection conn;
    String body;
    byte[] input;

    body      = Json.encode(payload);
    targetUrl = new URL(url);
    conn      = (HttpURLConnection) targetUrl.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + token);
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      input = body.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    conn.getResponseCode();
  }
}
