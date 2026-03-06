package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.PromemoriaDto;
import dev.crm.module.chiamate.service.PromemoriaService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/promemoria")
public class PromemoriaController
{

  private final PromemoriaService service;

  public PromemoriaController(PromemoriaService service)
  {
    this.service = service;
  }

  @GetMapping("/richiamo/{richiamoId}")
  public ApiPayload findByRichiamoId(@PathVariable Long richiamoId)
      throws Exception
  {
    return ApiResponse.create().out(service.findByRichiamoId(richiamoId)).build();
  }

  @GetMapping("/da-inviare")
  public ApiPayload findDaInviare() throws Exception
  {
    return ApiResponse.create().out(service.findDaInviare()).build();
  }

  @PostMapping
  public ApiPayload create(@RequestBody PromemoriaDto dto) throws Exception
  {
    return ApiResponse.create().out(service.create(dto)).status(200).build();
  }

  @PutMapping("/{id}/inviato")
  public ApiPayload markInviato(@PathVariable Long id) throws Exception
  {
    service.markInviato(id);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }

  @DeleteMapping("/{id}")
  public ApiPayload delete(@PathVariable Long id) throws Exception
  {
    service.delete(id);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }
}
