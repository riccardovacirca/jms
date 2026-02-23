package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.StatoContattoDto;
import dev.crm.module.chiamate.service.StatoContattoService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/esiti/stato-contatto")
public class StatoContattoController
{
  private final StatoContattoService service;

  public StatoContattoController(StatoContattoService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload aggiorna(@RequestBody StatoContattoDto dto)
  {
    return ApiResponse.create().out(service.aggiorna(dto)).status(200).build();
  }

  @GetMapping
  public ApiPayload findAll()
  {
    return ApiResponse.create().out(service.findAll()).build();
  }

  @GetMapping("/{contattoId}")
  public ApiPayload find(@PathVariable Long contattoId)
  {
    var result = service.find(contattoId);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Stato contatto non trovato")
        .status(200)
        .build();
  }

  @GetMapping("/stato/{stato}")
  public ApiPayload findByStato(@PathVariable String stato)
  {
    return ApiResponse.create().out(service.findByStato(stato)).build();
  }
}
