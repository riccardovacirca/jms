package dev.crm.module.operatori.controller;

import dev.crm.module.operatori.dto.OperatoreDto;
import dev.crm.module.operatori.service.OperatoreService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operatori")
public class OperatoreController
{

  private final OperatoreService service;

  public OperatoreController(OperatoreService service)
  {
    this.service = service;
  }

  @GetMapping
  public ApiPayload findAll(@RequestParam(required = false) String stato)
      throws Exception
  {
    if (stato != null && !stato.isEmpty()) {
      return ApiResponse.create().out(service.findByStato(stato)).build();
    }
    return ApiResponse.create().out(service.findAll()).build();
  }

  @GetMapping("/{id}")
  public ApiPayload findById(@PathVariable Long id) throws Exception
  {
    var result = service.findById(id);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Operatore non trovato")
        .status(200)
        .build();
  }

  @PostMapping
  public ApiPayload create(@RequestBody OperatoreDto dto) throws Exception
  {
    return ApiResponse.create().out(service.create(dto)).status(200).build();
  }

  @PutMapping("/{id}")
  public ApiPayload update(@PathVariable Long id, @RequestBody OperatoreDto dto)
      throws Exception
  {
    dto.id = id;
    return ApiResponse.create().out(service.update(dto)).build();
  }

  @DeleteMapping("/{id}")
  public ApiPayload delete(@PathVariable Long id) throws Exception
  {
    service.delete(id);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }

  @PutMapping("/{id}/stato")
  public ApiPayload updateStato(@PathVariable Long id, @RequestParam String stato)
      throws Exception
  {
    return ApiResponse.create().out(service.updateStato(id, stato)).build();
  }
}
