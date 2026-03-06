package dev.crm.module.logs.controller;

import dev.crm.module.logs.dto.LogCreateRequestDto;
import dev.crm.module.logs.dto.LogDto;
import dev.crm.module.logs.service.LogService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController
{
  private final LogService logService;

  public LogController(LogService logService)
  {
    this.logService = logService;
  }

  @GetMapping
  public ApiPayload findAll(
      @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset)
  {
    List<LogDto> logs;
    ApiPayload response;

    try {
      logs = logService.findAll(limit, offset);

      response = ApiResponse.create()
          .out(logs)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to fetch logs: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/module/{module}")
  public ApiPayload findByModule(
      @PathVariable String module,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(defaultValue = "0") int offset)
  {
    List<LogDto> logs;
    ApiPayload response;

    try {
      logs = logService.findByModule(module, limit, offset);

      response = ApiResponse.create()
          .out(logs)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to fetch logs by module: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/level/{level}")
  public ApiPayload findByLevel(
      @PathVariable String level,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(defaultValue = "0") int offset)
  {
    List<LogDto> logs;
    ApiPayload response;

    try {
      logs = logService.findByLevel(level, limit, offset);

      response = ApiResponse.create()
          .out(logs)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to fetch logs by level: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @GetMapping("/{id}")
  public ApiPayload findById(@PathVariable Long id)
  {
    LogDto log;
    ApiPayload response;

    try {
      log = logService.findById(id);

      if (log == null) {
        response = ApiResponse.create()
            .err(true)
            .log("Log not found")
            .status(200)
            .contentType("application/json")
            .build();

        return response;
      }

      response = ApiResponse.create()
          .out(log)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to fetch log: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @PostMapping
  public ApiPayload create(
      @Valid @RequestBody LogCreateRequestDto request, HttpServletRequest httpRequest)
  {
    LogDto log;
    ApiPayload response;
    Long userId;
    String ipAddress;
    String userAgent;

    try {
      // Estrai info dalla request HTTP
      userId = (Long) httpRequest.getAttribute("userId");
      ipAddress = getClientIpAddress(httpRequest);
      userAgent = httpRequest.getHeader("User-Agent");

      log = logService.create(request, userId, null, ipAddress, userAgent);

      response = ApiResponse.create()
          .out(log)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to create log: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @DeleteMapping("/{id}")
  public ApiPayload delete(@PathVariable Long id)
  {
    boolean deleted;
    ApiPayload response;

    try {
      deleted = logService.delete(id);

      if (!deleted) {
        response = ApiResponse.create()
            .err(true)
            .log("Log not found")
            .status(200)
            .contentType("application/json")
            .build();

        return response;
      }

      response = ApiResponse.create()
          .out(Map.of("success", true, "id", id))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to delete log: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @DeleteMapping("/cleanup/{days}")
  public ApiPayload cleanup(@PathVariable int days)
  {
    int deleted;
    ApiPayload response;

    try {
      deleted = logService.deleteOlderThan(days);

      response = ApiResponse.create()
          .out(Map.of("deleted", deleted, "days", days))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to cleanup logs: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  private String getClientIpAddress(HttpServletRequest request)
  {
    String ip;

    ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
