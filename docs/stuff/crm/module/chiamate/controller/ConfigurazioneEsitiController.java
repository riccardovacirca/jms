package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.ConfigurazioneEsitiDto;
import dev.crm.module.chiamate.service.ConfigurazioneEsitiService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/esiti/config")
public class ConfigurazioneEsitiController
{
  private final ConfigurazioneEsitiService service;

  public ConfigurazioneEsitiController(ConfigurazioneEsitiService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload salva(@RequestBody ConfigurazioneEsitiDto dto)
  {
    ConfigurazioneEsitiDto created = service.salva(dto);
    return ApiResponse.create().out(created).status(200).build();
  }

  @GetMapping
  public ApiPayload findAll()
  {
    List<ConfigurazioneEsitiDto> data = service.findAll();
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/attivi")
  public ApiPayload findAttivi()
  {
    List<ConfigurazioneEsitiDto> data = service.findAttivi();
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/{codice}")
  public ApiPayload find(@PathVariable String codice)
  {
    var result = service.get(codice);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Configurazione esiti non trovata")
        .status(200)
        .build();
  }

  @DeleteMapping("/{codice}")
  public ApiPayload delete(@PathVariable String codice)
  {
    service.delete(codice);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }
}
