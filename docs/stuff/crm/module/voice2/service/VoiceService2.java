package dev.crm.module.voice2.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.crm.module.cloud.dto.InstallationMetadataDto;
import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.voice.config.VoiceConfig;
import dev.crm.module.voice.dao.CallDao;
import dev.crm.module.voice.dto.CallDto;
import dev.springtools.util.JSON;
import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VoiceService2
{

  private static final Logger log = LoggerFactory.getLogger(VoiceService2.class);

  private final VoiceConfig config;
  private final CallDao callDao;
  private final InstallationService installationService;

  // In-memory storage for pending calls: userId -> customerNumber
  // Assumption: one operator can have only one pending call at a time
  private final Map<String, String> pendingCalls = new java.util.concurrent.ConcurrentHashMap<>();

  // Track relationship between operator call and customer call for hangup
  // operatorCallId -> customerCallId
  private final Map<String, String> operatorToCustomerCalls = new java.util.concurrent.ConcurrentHashMap<>();

  public VoiceService2(VoiceConfig config, CallDao callDao, InstallationService installationService)
  {
    this.config = config;
    this.callDao = callDao;
    this.installationService = installationService;
  }

  /**
   * Prepare a call by storing the customer number for later retrieval.
   * This is called by the frontend BEFORE serverCall().
   */
  public void prepareCall(String userId, String customerNumber)
  {
    log.info("=== PREPARE CALL ===");
    log.info("UserId: {}", userId);
    log.info("Customer Number: {}", customerNumber);

    pendingCalls.put(userId, customerNumber);

    log.info("Pending call stored. Total pending: {}", pendingCalls.size());
  }

  /**
   * Get pending customer number for a user.
   * Called by answer webhook to retrieve the customer number.
   */
  public String getPendingCustomerNumber(String userId)
  {
    String customerNumber;

    customerNumber = pendingCalls.remove(userId);

    log.info("Retrieved pending customer number for userId={}: {}", userId, customerNumber);

    return customerNumber;
  }

  /**
   * Build NCCO for operator to join conversation with hold music.
   * Operator waits with startOnEnter: false until customer joins.
   */
  public List<Map<String, Object>> buildOperatorWaitingNcco(String conversationName, String musicOnHoldUrl)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;

    ncco = new ArrayList<>();
    conversation = new HashMap<>();
    conversation.put("action", "conversation");
    conversation.put("name", conversationName);
    conversation.put("startOnEnter", false);
    conversation.put("musicOnHoldUrl", List.of(musicOnHoldUrl));

    ncco.add(conversation);

    return ncco;
  }

  /**
   * Build NCCO for customer to join conversation.
   * Customer joins with default startOnEnter: true, starting the conversation.
   */
  public List<Map<String, Object>> buildCustomerJoinNcco(String conversationName)
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> conversation;

    ncco = new ArrayList<>();
    conversation = new HashMap<>();
    conversation.put("action", "conversation");
    conversation.put("name", conversationName);

    ncco.add(conversation);

    return ncco;
  }

  /**
   * Call customer via Vonage Voice API to join existing conversation.
   */
  public void callCustomer(String customerNumber, String conversationName, String operatorCallUuid) throws Exception
  {
    Map<String, Object> payload;
    String token;
    String responseBody;
    Map<String, Object> responseMap;
    String uuid;
    String status;
    String direction;
    String conversationUuid;
    CallDto callDto;
    List<Map<String, Object>> ncco;
    String eventUrl;
    String installationToken;
    String installationId;
    InstallationMetadataDto metadata;

    log.info("[CALL_CUSTOMER] to={}, conversation={}", customerNumber, conversationName);

    // Build dynamic event_url with installation_id and token
    metadata = installationService.getOrCreateInstallation();
    installationId = metadata.installationId;

    installationToken = installationService.generateEventUrlToken(conversationName);

    eventUrl = config.getEventUrl()
      + "?installation_id=" + installationId
      + "&conversation_uuid=" + conversationName
      + "&token=" + installationToken;

    log.info("[EVENT_URL] installation_id={}, url={}", installationId, eventUrl);

    ncco = buildCustomerJoinNcco(conversationName);

    payload = new HashMap<>();
    payload.put("to", List.of(Map.of("type", "phone", "number", customerNumber)));
    payload.put("from", Map.of("type", "phone", "number", config.getFromNumber()));
    payload.put("ncco", ncco);
    payload.put("event_url", List.of(eventUrl));

    log.debug("Payload: {}", payload);

    token = resolveToken();

    log.info("Sending POST to Vonage API...");
    responseBody = postJson(config.getBaseUrl(), payload, token);

    responseMap = (Map<String, Object>) JSON.decode(responseBody, Map.class);

    uuid = (String) responseMap.get("uuid");
    status = (String) responseMap.get("status");
    direction = (String) responseMap.get("direction");
    conversationUuid = (String) responseMap.get("conversation_uuid");

    log.info("[CALL_CREATED] uuid={}, conversation_uuid={}, status={}", uuid, conversationUuid, status);

    callDto = new CallDto();
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

    // Store relationship between operator and customer calls for hangup
    operatorToCustomerCalls.put(operatorCallUuid, uuid);
    log.info("Stored call relationship: operator={} -> customer={}", operatorCallUuid, uuid);
  }

  /**
   * Hangup call by UUID.
   * If this is an operator call, also hangup the associated customer call.
   */
  public void hangupCall(String operatorCallUuid) throws Exception
  {
    String url;
    String token;
    Map<String, Object> payload;
    String customerCallUuid;

    log.info("=== HANGUP CALL ===");
    log.info("Operator call UUID: {}", operatorCallUuid);

    token = resolveToken();
    payload = new HashMap<>();
    payload.put("action", "hangup");

    // Hangup operator call
    url = config.getBaseUrl() + "/" + operatorCallUuid;
    log.info("Hanging up operator call: {}", operatorCallUuid);
    putJson(url, payload, token);

    // Check if there's an associated customer call and hangup that too
    customerCallUuid = operatorToCustomerCalls.remove(operatorCallUuid);
    if (customerCallUuid != null) {
      log.info("Found associated customer call: {}", customerCallUuid);
      url = config.getBaseUrl() + "/" + customerCallUuid;
      log.info("Hanging up customer call: {}", customerCallUuid);
      putJson(url, payload, token);
    } else {
      log.warn("No associated customer call found for operator: {}", operatorCallUuid);
    }

    log.info("=== HANGUP COMPLETED ===");
  }

  private String resolveToken() throws Exception
  {
    String token;

    token = config.getToken();

    if (token == null || token.isEmpty()) {
      token = generateJwt();
    }

    return token;
  }

  private String generateJwt() throws Exception
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

  private void putJson(String url, Map<String, Object> payload, String token) throws Exception
  {
    URL targetUrl;
    HttpURLConnection conn;
    String body;
    byte[] input;
    String responseBody;

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
      responseBody = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
    }
  }

  /**
   * Generate SDK JWT for Client SDK authentication.
   * Used by browser/mobile app to authenticate as a specific user.
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
    claims.put("exp", now + 3600);
    claims.put("jti", UUID.randomUUID().toString());
    claims.put("sub", userId);
    claims.put("acl", Map.of("paths", aclPaths));

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
      pemContent = Files.readString(Paths.get(configValue));
    } else {
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
}
