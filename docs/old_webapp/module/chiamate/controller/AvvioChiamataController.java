package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.AvvioChiamataDto;
import dev.crm.module.chiamate.service.AvvioChiamataService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate")
public class AvvioChiamataController
{
  private final AvvioChiamataService service;

  public AvvioChiamataController(AvvioChiamataService service)
  {
    this.service = service;
  }

  @PostMapping("/avvio")
  public ApiPayload avvia(@RequestBody AvvioChiamataDto dto)
  {
    AvvioChiamataDto created = service.avvia(dto);
    return ApiResponse.create().out(created).status(200).build();
  }

  @GetMapping
  public ApiPayload list()
  {
    List<AvvioChiamataDto> data = service.findAll();
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/{id}")
  public ApiPayload get(@PathVariable Long id)
  {
    var result = service.find(id);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Chiamata non trovata")
        .status(200)
        .build();
  }

  @GetMapping("/operatore/{operatoreId}")
  public ApiPayload byOperatore(@PathVariable Long operatoreId)
  {
    List<AvvioChiamataDto> data = service.findByOperatore(operatoreId);
    return ApiResponse.create().out(data).build();
  }

  @PostMapping("/{id}/termina")
  public ApiPayload termina(@PathVariable Long id)
  {
    if (service.find(id).isEmpty()) {
      return ApiResponse.create()
          .err(true)
          .log("Chiamata non trovata")
          .status(200)
          .build();
    }
    service.termina(id);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }
}
