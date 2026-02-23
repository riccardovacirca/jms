package dev.crm.module.voice.service;

import dev.crm.module.cloud.dto.InstallationMetadataDto;
import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.voice.config.VoiceConfig;
import dev.crm.module.voice.dao.CallDao;
import dev.crm.module.voice.dao.CallEventDao;
import dev.crm.module.voice.dto.CallDto;
import dev.crm.module.voice.dto.CallEventDto;
import dev.crm.module.voice.dto.CreateCallRequestDto;
import dev.crm.module.voice.dto.CreateCallResponseDto;
import dev.springtools.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class VoiceService
{
  private static final Logger log = LoggerFactory.getLogger(VoiceService.class);

  private final VoiceConfig config;
  private final CallDao callDao;
  private final CallEventDao callEventDao;
  private final InstallationService installationService;

  public VoiceService(
    VoiceConfig config,
    CallDao callDao,
    CallEventDao callEventDao,
    InstallationService installationService
  )
  {
    this.config = config;
    this.callDao = callDao;
    this.callEventDao = callEventDao;
    this.installationService = installationService;
  }

  // ---------------------------------------------------------------------------
  // Test call: dials config.testNumber, plays TTS, no operator connection.
  // Used for system-level verification that the voice pipeline works.
  // ---------------------------------------------------------------------------

  public CreateCallResponseDto testCall() throws Exception
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> customData;

    ncco = buildTestNcco();
    customData = new HashMap<>();

    return submitCall(config.getTestNumber(), ncco, customData);
  }

  private List<Map<String, Object>> buildTestNcco()
  {
    return List.of(
        Map.of(
            "action", "talk",
            "text", "Benvenuto. Questa è una chiamata di test dal CRM.",
            "language", "it-IT"));
  }

  // ---------------------------------------------------------------------------
  // Real call: dials the customer (toNumber), connects them to the operator.
  // No TTS — the operator speaks directly to the customer.
  // ---------------------------------------------------------------------------

  public CreateCallResponseDto createCall(CreateCallRequestDto request) throws Exception
  {
    List<Map<String, Object>> ncco;
    Map<String, Object> customData;

    ncco = buildConnectNcco(request.operatorType, request.operatorId);
    customData = buildCustomData(request.operatorIdCrm, request.campagnaId, request.contattoId);

    return submitCall(request.toNumber, ncco, customData);
  }

  private Map<String, Object> buildCustomData(Long operatorId, Long campagnaId, Long contattoId)
  {
    Map<String, Object> customData;

    customData = new HashMap<>();

    if (operatorId != null) {
      customData.put("operator_id", operatorId);
    }
    if (campagnaId != null) {
      customData.put("campagna_id", campagnaId);
    }
    if (contattoId != null) {
      customData.put("contatto_id", contattoId);
    }

    return customData;
  }

  private List<Map<String, Object>> buildConnectNcco(String operatorType, String operatorId)
  {
    Map<String, Object> endpoint;
    Map<String, Object> connectAction;

    if (operatorType.equals("app")) {
      endpoint = Map.of("type", "app", "user", operatorId);
    } else {
      endpoint = Map.of("type", "phone", "number", operatorId);
    }

    connectAction = Map.of(
        "action", "connect",
        "from", config.getFromNumber(),
        "endpoint", List.of(endpoint));

    return List.of(connectAction);
  }


  // ---------------------------------------------------------------------------
  // Shared: builds the Vonage create-call payload, POSTs it, persists the result.
  // ---------------------------------------------------------------------------

  private CreateCallResponseDto submitCall(
      String toNumber,
      List<Map<String, Object>> ncco,
      Map<String, Object> customData) throws Exception
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
    CreateCallResponseDto result;
    Long operatorId;
    Long campagnaId;
    Long contattoId;

    // Build dynamic event_url with installation_id and token BEFORE making the call
    String eventUrl;
    String installationToken;
    String installationId;
    InstallationMetadataDto metadata;

    log.info("[CALL_CREATE] to={}", toNumber);

    metadata = installationService.getOrCreateInstallation();
    installationId = metadata.installationId;

    // Generate token with placeholder conversation_uuid (will be available in webhook payload)
    installationToken = installationService.generateEventUrlToken("pending");

    eventUrl = config.getEventUrl()
      + "?installation_id=" + installationId
      + "&token=" + installationToken;

    log.info("[EVENT_URL] installation_id={}, url={}", installationId, eventUrl);

    payload = new HashMap<>();
    payload.put("to", List.of(Map.of("type", "phone", "number", toNumber)));
    payload.put("from", Map.of("type", "phone", "number", config.getFromNumber()));
    payload.put("ncco", ncco);
    payload.put("event_url", List.of(eventUrl));

    if (customData != null && !customData.isEmpty()) {
      payload.put("custom_data", customData);
    }

    token = resolveToken();
    responseBody = postJson(config.getBaseUrl(), payload, token);

    responseMap = (Map<String, Object>) JSON.decode(responseBody, Map.class);

    uuid = (String) responseMap.get("uuid");
    status = (String) responseMap.get("status");
    direction = (String) responseMap.get("direction");
    conversationUuid = (String) responseMap.get("conversation_uuid");

    log.info("[CALL_CREATED] uuid={}, conversation_uuid={}, status={}", uuid, conversationUuid, status);

    operatorId = null;
    campagnaId = null;
    contattoId = null;

    if (customData != null) {
      operatorId = customData.containsKey("operator_id")
          ? ((Number) customData.get("operator_id")).longValue()
          : null;
      campagnaId = customData.containsKey("campagna_id")
          ? ((Number) customData.get("campagna_id")).longValue()
          : null;
      contattoId = customData.containsKey("contatto_id")
          ? ((Number) customData.get("contatto_id")).longValue()
          : null;
    }

    callDto = new CallDto();
    callDto.uuid = uuid;
    callDto.conversationUuid = conversationUuid;
    callDto.status = status;
    callDto.direction = direction;
    callDto.fromType = "phone";
    callDto.fromNumber = config.getFromNumber();
    callDto.toType = "phone";
    callDto.toNumber = toNumber;
    callDto.eventUrl = eventUrl;
    callDto.operatorId = operatorId;
    callDto.campagnaId = campagnaId;
    callDto.contattoId = contattoId;
    callDto.createdAt = LocalDateTime.now();

    callDao.insert(callDto);

    result = new CreateCallResponseDto(uuid, status, direction, conversationUuid);

    return result;
  }

  // ---------------------------------------------------------------------------
  // Read operations
  // ---------------------------------------------------------------------------

  public List<CallDto> listCalls() throws Exception
  {
    List<CallDto> calls;

    calls = callDao.findAll();

    return calls;
  }

  public Optional<CallDto> getCall(String uuid) throws Exception
  {
    Optional<CallDto> call;

    call = callDao.findByUuid(uuid);

    return call;
  }

  public List<CallEventDto> getCallEvents(Long callId) throws Exception
  {
    List<CallEventDto> events;

    events = callEventDao.findByCallId(callId);

    return events;
  }

  public void hangupCall(String uuid) throws Exception
  {
    String url;
    String token;
    Map<String, Object> payload;
    String responseBody;

    url = config.getBaseUrl() + "/" + uuid;
    token = resolveToken();

    payload = new HashMap<>();
    payload.put("action", "hangup");

    putJson(url, payload, token);
  }

  private String putJson(String url, Map<String, Object> payload, String token) throws Exception
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
      responseBody = sc.useDelimiter("\\A").next();
    }

    return responseBody;
  }

  // ---------------------------------------------------------------------------
  // Webhook event handler
  // ---------------------------------------------------------------------------

  public void handleWebhookEvent(Map<String, Object> payload) throws Exception
  {
    String uuid;
    String status;
    String conversationUuid;
    Optional<CallDto> existingCall;

    uuid = (String) payload.get("uuid");
    status = (String) payload.get("status");
    conversationUuid = (String) payload.get("conversation_uuid");

    existingCall = callDao.findByUuid(uuid);

    if (existingCall.isEmpty()) {
      CallDto callDto;
      Map<String, Object> from;
      Map<String, Object> to;
      Map<String, Object> customData;
      Object fromRaw;
      Object toRaw;
      Long operatorId;
      Long campagnaId;
      Long contattoId;
      long callId;

      callDto = new CallDto();
      callDto.uuid = uuid;
      callDto.conversationUuid = conversationUuid;
      callDto.status = status;
      callDto.direction = (String) payload.get("direction");

      fromRaw = payload.get("from");
      if (fromRaw instanceof Map) {
        from = (Map<String, Object>) fromRaw;
        callDto.fromType = (String) from.get("type");
        callDto.fromNumber = (String) from.get("number");
      } else if (fromRaw instanceof String) {
        callDto.fromNumber = (String) fromRaw;
      }

      toRaw = payload.get("to");
      if (toRaw instanceof Map) {
        to = (Map<String, Object>) toRaw;
        callDto.toType = (String) to.get("type");
        callDto.toNumber = (String) to.get("number");
      } else if (toRaw instanceof String) {
        callDto.toNumber = (String) toRaw;
      }

      customData = (Map<String, Object>) payload.get("custom_data");
      operatorId = null;
      campagnaId = null;
      contattoId = null;

      if (customData != null) {
        operatorId = customData.containsKey("operator_id")
            ? ((Number) customData.get("operator_id")).longValue()
            : null;
        campagnaId = customData.containsKey("campagna_id")
            ? ((Number) customData.get("campagna_id")).longValue()
            : null;
        contattoId = customData.containsKey("contatto_id")
            ? ((Number) customData.get("contatto_id")).longValue()
            : null;
      }

      callDto.operatorId = operatorId;
      callDto.campagnaId = campagnaId;
      callDto.contattoId = contattoId;
      callDto.createdAt = LocalDateTime.now();

      callId = callDao.insert(callDto);

      saveEvent(callId, payload);
    } else {
      callDao.updateStatus(uuid, status);

      saveEvent(existingCall.get().id, payload);
    }
  }


  private void saveEvent(Long callId, Map<String, Object> payload) throws Exception
  {
    CallEventDto eventDto;
    String timestampStr;
    Object fromRaw;
    Object toRaw;
    Map<String, Object> from;
    Map<String, Object> to;

    eventDto = new CallEventDto();
    eventDto.callId = callId;
    eventDto.uuid = (String) payload.get("uuid");
    eventDto.conversationUuid = (String) payload.get("conversation_uuid");
    eventDto.status = (String) payload.get("status");
    eventDto.direction = (String) payload.get("direction");

    timestampStr = (String) payload.get("timestamp");
    if (timestampStr != null) {
      eventDto.timestamp = LocalDateTime.ofInstant(Instant.parse(timestampStr), ZoneId.systemDefault());
    }

    fromRaw = payload.get("from");
    if (fromRaw instanceof Map) {
      from = (Map<String, Object>) fromRaw;
      eventDto.fromNumber = (String) from.get("number");
    } else if (fromRaw instanceof String) {
      eventDto.fromNumber = (String) fromRaw;
    }

    toRaw = payload.get("to");
    if (toRaw instanceof Map) {
      to = (Map<String, Object>) toRaw;
      eventDto.toNumber = (String) to.get("number");
    } else if (toRaw instanceof String) {
      eventDto.toNumber = (String) toRaw;
    }

    eventDto.payload = JSON.encode(payload);
    eventDto.createdAt = LocalDateTime.now();

    callEventDao.insert(eventDto);
  }

  // ---------------------------------------------------------------------------
  // Token resolution: returns the token from config if present,
  // otherwise generates an RS256 JWT from the private key.
  // ---------------------------------------------------------------------------

  private String resolveToken() throws Exception
  {
    String token;

    token = config.getToken();

    if (token != null && !token.isEmpty()) {
      return token;
    }

    return generateJwt();
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

    // Se configValue è un path (non contiene BEGIN PRIVATE KEY), leggi il file
    if (!configValue.contains("BEGIN PRIVATE KEY")) {
      pemContent = java.nio.file.Files.readString(java.nio.file.Path.of(configValue));
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

  // API JWT: used to authenticate requests to the Vonage Voice API.
  // exp = 5 minutes.

  private String generateJwt() throws Exception
  {
    PrivateKey privateKey;
    long now;
    String header;
    String payload;
    Signature signer;
    byte[] signatureBytes;
    String signature;
    String token;

    privateKey = loadPrivateKey();
    now = Instant.now().getEpochSecond();

    header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(String.format(
            "{\"application_id\":\"%s\",\"iat\":%d,\"exp\":%d,\"jti\":\"%s\"}",
            config.getApplicationId(), now, now + 300, UUID.randomUUID()
        ).getBytes(StandardCharsets.UTF_8));

    signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
    signatureBytes = signer.sign();

    signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

    token = header + "." + payload + "." + signature;

    return token;
  }

  // SDK JWT: used by the Vonage Client SDK in the browser to authenticate
  // as a specific user. Includes "sub" (user ID) and "acl" (allowed paths).
  // exp = 1 hour. ACL paths: docs/vonage/sdk/doc6.md

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

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

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
}
