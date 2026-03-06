package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.EsitoDto;
import dev.crm.module.chiamate.service.EsitoChiusuraService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/esiti")
public class EsitoChiusuraController
{
  private final EsitoChiusuraService service;

  public EsitoChiusuraController(EsitoChiusuraService service)
  {
    this.service = service;
  }

  @PostMapping("/chiusura")
  public ApiPayload chiudi(@RequestBody EsitoDto dto)
  {
    try {
      EsitoDto saved = service.chiudi(dto);
      return ApiResponse.create().out(saved).status(200).build();
    } catch (IllegalArgumentException e) {
      return ApiResponse.create()
          .err(true)
          .log(e.getMessage())
          .status(200)
          .build();
    }
  }

  @GetMapping
  public ApiPayload findAll()
  {
    return ApiResponse.create().out(service.findAll()).build();
  }

  @GetMapping("/{id}")
  public ApiPayload find(@PathVariable Long id)
  {
    var result = service.find(id);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Esito non trovato")
        .status(200)
        .build();
  }

  @GetMapping("/chiamata/{chiamataId}")
  public ApiPayload findByChiamata(@PathVariable Long chiamataId)
  {
    return ApiResponse.create().out(service.findByChiamata(chiamataId)).build();
  }

  @GetMapping("/contatto/{contattoId}")
  public ApiPayload findByContatto(@PathVariable Long contattoId)
  {
    return ApiResponse.create().out(service.findByContatto(contattoId)).build();
  }
}
