package dev.crm.module.status.controller;

import dev.crm.module.status.dto.LogRequestDto;
import dev.crm.module.status.dto.StatusHealthDto;
import dev.crm.module.status.dto.StatusLogDto;
import dev.crm.module.status.service.StatusService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/status")
public class StatusController
{
  private final StatusService service;

  public StatusController(StatusService service)
  {
    this.service = service;
  }

  @GetMapping("/health")
  public ApiPayload health()
  {
    StatusHealthDto data;
    ApiPayload response;

    data = service.getHealth();
    response = ApiResponse
      .create()
      .out(data)
      .contentType("application/json")
      .build();

    return response;
  }

  @PostMapping("/log")
  public ApiPayload
  log(@Valid @RequestBody LogRequestDto request) throws Exception
  {
    StatusLogDto log;
    ApiPayload response;
    String message;

    message = request.getMessage();
    log = service.log(message);
    response = ApiResponse
      .create()
      .out(log)
      .status(200)
      .contentType("application/json")
      .build();

    return response;
  }

  @GetMapping("/logs")
  public ApiPayload
  logs(@RequestParam(defaultValue = "10") int num,
       @RequestParam(defaultValue = "0") int off) throws Exception
  {
    List<StatusLogDto> logs;
    ApiPayload response;

    logs = service.getLogs(num, off);
    response = ApiResponse
      .create()
      .out(logs)
      .contentType("application/json")
      .build();

    return response;
  }
}
