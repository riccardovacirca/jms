package dev.crm.module.voice.controller;

import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.voice.dto.CallDto;
import dev.crm.module.voice.dto.CallEventDto;
import dev.crm.module.voice.dto.CreateCallRequestDto;
import dev.crm.module.voice.dto.CreateCallResponseDto;
import dev.crm.module.voice.service.VoiceService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
public class VoiceController
{
  private static final Logger log = LoggerFactory.getLogger(VoiceController.class);

  private final VoiceService voiceService;
  private final InstallationService installationService;

  public VoiceController(VoiceService voiceService, InstallationService installationService)
  {
    this.voiceService = voiceService;
    this.installationService = installationService;
  }

  @PostMapping("/test")
  public ApiPayload testCall()
  {
    ApiPayload response;

    try {
      CreateCallResponseDto callResponse;

      callResponse = voiceService.testCall();

      response = ApiResponse
          .create()
          .out(callResponse)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to create test call: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @PostMapping("/calls")
  public ApiPayload createCall(@RequestBody CreateCallRequestDto dto)
  {
    ApiPayload response;

    try {
      CreateCallResponseDto callResponse;

      callResponse = voiceService.createCall(dto);

      response = ApiResponse
          .create()
          .out(callResponse)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to create call: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/calls")
  public ApiPayload listCalls()
  {
    ApiPayload response;

    try {
      List<CallDto> calls;

      calls = voiceService.listCalls();

      response = ApiResponse
          .create()
          .out(calls)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to list calls: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/calls/{uuid}")
  public ApiPayload getCall(@PathVariable String uuid)
  {
    ApiPayload response;

    try {
      Optional<CallDto> call;

      call = voiceService.getCall(uuid);

      if (call.isEmpty()) {
        response = ApiResponse
            .create()
            .err(true)
            .log("Call not found")
            .status(200)
            .contentType("application/json")
            .build();

        return response;
      }

      response = ApiResponse
          .create()
          .out(call.get())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to get call: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/calls/{id}/events")
  public ApiPayload getCallEvents(@PathVariable Long id)
  {
    ApiPayload response;

    try {
      List<CallEventDto> events;

      events = voiceService.getCallEvents(id);

      response = ApiResponse
          .create()
          .out(events)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to get call events: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

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

  @PostMapping("/webhook/event")
  public ApiPayload handleEventWebhook(
      @RequestParam(required = false) String installation_id,
      @RequestParam(required = false) String token,
      @RequestBody Map<String, Object> payload)
  {
    ApiPayload response;
    String conversationUuid;
    String status;

    conversationUuid = (String) payload.get("conversation_uuid");
    status = (String) payload.get("status");

    try {
      log.info("[WEBHOOK_RECEIVED] installation_id={}, conversation_uuid={}, status={}",
        installation_id, conversationUuid, status);

      // Validate token if present
      boolean isTokenValid;

      isTokenValid = true;

      if (installation_id != null && token != null) {
        isTokenValid = installationService.validateEventUrlToken(token, installation_id);

        if (!isTokenValid) {
          log.error("[WEBHOOK_VALIDATION] result=FAIL, installation_id={}", installation_id);

          response = ApiResponse
              .create()
              .err(true)
              .log("Invalid token")
              .status(200)
              .contentType("application/json")
              .build();

          return response;
        }

        log.info("[WEBHOOK_VALIDATION] result=SUCCESS, installation_id={}", installation_id);
      } else {
        log.warn("[WEBHOOK_VALIDATION] no_token_provided, using_legacy_mode");
      }

      voiceService.handleWebhookEvent(payload);

      log.info("[WEBHOOK_PROCESSED] installation_id={}, conversation_uuid={}", installation_id, conversationUuid);

      response = ApiResponse
          .create()
          .out(Map.of("success", true, "token_valid", isTokenValid))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      log.error("[WEBHOOK_ERROR] installation_id={}, error={}", installation_id, e.getMessage(), e);

      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to handle webhook event: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @PutMapping("/calls/{uuid}/hangup")
  public ApiPayload hangupCall(@PathVariable String uuid)
  {
    ApiPayload response;

    try {
      voiceService.hangupCall(uuid);

      response = ApiResponse
          .create()
          .out(Map.of("success", true))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Failed to hangup call: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }
}
