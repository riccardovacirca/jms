package dev.crm.module.voice2.controller;

import dev.crm.module.voice2.service.VoiceService2;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice2")
public class VoiceController2
{

  private static final Logger log = LoggerFactory.getLogger(VoiceController2.class);

  private final VoiceService2 voiceService;

  public VoiceController2(VoiceService2 voiceService)
  {
    this.voiceService = voiceService;
  }

  /**
   * Prepare a call by storing customer number for later use.
   * Frontend calls this BEFORE serverCall().
   */
  @PostMapping("/prepare-call")
  public ApiPayload prepareCall(@RequestBody Map<String, String> request)
  {
    ApiPayload response;
    String userId;
    String customerNumber;

    userId = request.get("userId");
    customerNumber = request.get("customerNumber");

    if (userId == null || customerNumber == null) {
      response = ApiResponse
          .create()
          .err(true)
          .log("userId and customerNumber are required")
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }

    voiceService.prepareCall(userId, customerNumber);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Call prepared"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Answer URL webhook for operator serverCall().
   * Returns NCCO to put operator in conversation with hold music.
   * Triggers customer call asynchronously after returning NCCO.
   *
   * This endpoint must be configured as answer_url in Vonage application.
   */
  @PostMapping("/answer")
  public ResponseEntity<List<Map<String, Object>>> handleAnswerWebhook(
      @RequestBody Map<String, Object> allParams)
  {
    List<Map<String, Object>> ncco;
    String conversationName;
    String musicOnHoldUrl;
    String customerNumber;
    String userId;
    String operatorCallUuid;

    log.info("=== VOICE2 ANSWER WEBHOOK CALLED ===");
    log.info("ALL PARAMETERS: {}", allParams);

    // Try to extract userId from parameters
    // Vonage passes operator userId in 'from_user' field for serverCall()
    Object fromUserObj = allParams.get("from_user");
    Object toObj = allParams.get("to");
    Object fromObj = allParams.get("from");
    Object userIdObj = allParams.get("user_id");

    // Extract userId from various possible fields (prioritize from_user for serverCall)
    if (fromUserObj != null && !fromUserObj.toString().isEmpty()) {
      userId = fromUserObj.toString();
    } else if (toObj != null && !toObj.toString().isEmpty()) {
      userId = toObj.toString();
    } else if (userIdObj != null && !userIdObj.toString().isEmpty()) {
      userId = userIdObj.toString();
    } else if (fromObj != null && !fromObj.toString().isEmpty()) {
      userId = fromObj.toString();
    } else {
      userId = null;
    }

    log.info("Extracted userId: {}", userId);

    // Extract operator call UUID
    Object uuidObj = allParams.get("uuid");
    operatorCallUuid = uuidObj != null ? uuidObj.toString() : null;
    log.info("Operator call UUID: {}", operatorCallUuid);

    // Try to retrieve customer number from pending calls
    if (userId != null) {
      customerNumber = voiceService.getPendingCustomerNumber(userId);
      log.info("Retrieved customer number from pending: {}", customerNumber);
    } else {
      customerNumber = null;
      log.warn("WARNING: Could not extract userId from webhook parameters!");
    }

    conversationName = "call-" + UUID.randomUUID().toString();
    musicOnHoldUrl = "https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3";

    log.info("Conversation Name: {}", conversationName);

    ncco = voiceService.buildOperatorWaitingNcco(conversationName, musicOnHoldUrl);

    log.debug("NCCO built: {}", ncco);

    // Trigger customer call asynchronously if customerNumber found
    if (customerNumber != null && !customerNumber.isEmpty() && operatorCallUuid != null) {
      log.info("Triggering async customer call to: {}", customerNumber);
      String finalConversationName = conversationName;
      String finalOperatorCallUuid = operatorCallUuid;
      new Thread(() -> {
        try {
          log.info("Async thread started - waiting 1s before calling customer...");
          Thread.sleep(1000);
          log.info("Calling customer now: {}", customerNumber);
          voiceService.callCustomer(customerNumber, finalConversationName, finalOperatorCallUuid);
          log.info("Customer call API executed successfully");
        } catch (Exception e) {
          log.error("ERROR: Failed to trigger customer call: {}", e.getMessage(), e);
        }
      }).start();
    } else {
      log.warn("WARNING: customerNumber not found or operatorCallUuid missing - customer call NOT triggered!");
    }

    log.info("=== ANSWER WEBHOOK RESPONSE SENT ===");
    return ResponseEntity.ok(ncco);
  }

  /**
   * Trigger customer call to join conversation.
   * Called after operator has joined the conversation and is ready.
   *
   * Request body:
   * {
   *   "customerNumber": "+39XXXXXXXXXX",
   *   "conversationName": "call-<uuid>",
   *   "operatorCallUuid": "<uuid>" (optional)
   * }
   */
  @PostMapping("/trigger-customer-call")
  public ApiPayload triggerCustomerCall(
      @RequestBody Map<String, String> request) throws Exception
  {
    ApiPayload response;
    String customerNumber;
    String conversationName;
    String operatorCallUuid;

    customerNumber = request.get("customerNumber");
    conversationName = request.get("conversationName");
    operatorCallUuid = request.get("operatorCallUuid");

    if (customerNumber == null || conversationName == null) {
      response = ApiResponse
          .create()
          .err(true)
          .log("customerNumber and conversationName are required")
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }

    voiceService.callCustomer(customerNumber, conversationName, operatorCallUuid);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Customer call triggered"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Hangup call by UUID.
   */
  @PutMapping("/calls/{uuid}/hangup")
  public ApiPayload hangupCall(@PathVariable String uuid) throws Exception
  {
    ApiPayload response;

    voiceService.hangupCall(uuid);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Call hangup triggered"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Generate SDK JWT token for Client SDK authentication.
   * Used by browser to authenticate as specific operator user.
   */
  @GetMapping("/sdk-token")
  public ApiPayload getSdkToken(@RequestParam String userId)
  {
    ApiPayload response;

    try {
      String token;

      token = voiceService.generateSdkJwt(userId);

      response = ApiResponse
          .create()
          .out(Map.of("token", token))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to generate SDK token: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }
}
