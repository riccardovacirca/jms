package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.StoricoRichiamoDto;
import dev.crm.module.chiamate.service.StoricoService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/storico-richiami")
public class StoricoController
{

  private final StoricoService service;

  public StoricoController(StoricoService service)
  {
    this.service = service;
  }

  @GetMapping("/richiamo/{richiamoId}")
  public ApiPayload findByRichiamoId(@PathVariable Long richiamoId) throws Exception
  {
    List<StoricoRichiamoDto> data = service.findByRichiamoId(richiamoId);
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/operatore/{operatoreId}")
  public ApiPayload findByOperatoreId(
      @PathVariable Long operatoreId,
      @RequestParam(required = false, defaultValue = "50") Integer limit)
      throws Exception
  {
    List<StoricoRichiamoDto> data = service.findByOperatoreId(operatoreId, limit);
    return ApiResponse.create().out(data).build();
  }
}
