package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.StatoChiamataDto;
import dev.crm.module.chiamate.service.StatoChiamataService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/stato")
public class StatoChiamataController
{
  private final StatoChiamataService service;

  public StatoChiamataController(StatoChiamataService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload aggiorna(@RequestBody StatoChiamataDto dto)
  {
    service.aggiorna(dto);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }

  @GetMapping("/{chiamataId}")
  public ApiPayload getStato(@PathVariable Long chiamataId)
  {
    var result = service.getStato(chiamataId);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Stato chiamata non trovato")
        .status(200)
        .build();
  }
}
