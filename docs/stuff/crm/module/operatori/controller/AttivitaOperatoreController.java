package dev.crm.module.operatori.controller;

import dev.crm.module.operatori.dto.AttivitaOperatoreDto;
import dev.crm.module.operatori.service.AttivitaOperatoreService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operatori")
public class AttivitaOperatoreController
{

  private final AttivitaOperatoreService service;

  public AttivitaOperatoreController(AttivitaOperatoreService service)
  {
    this.service = service;
  }

  @GetMapping("/{operatoreId}/attivita")
  public ApiPayload findAttivita(
      @PathVariable Long operatoreId,
      @RequestParam(required = false, defaultValue = "50") Integer limit)
      throws Exception
  {
    List<AttivitaOperatoreDto> data = service.findByOperatoreId(operatoreId, limit);
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/attivita")
  public ApiPayload findAllAttivita(
      @RequestParam(required = false, defaultValue = "100") Integer limit) throws Exception
  {
    List<AttivitaOperatoreDto> data = service.findAll(limit);
    return ApiResponse.create().out(data).build();
  }

  @PostMapping("/{operatoreId}/attivita")
  public ApiPayload registraAttivita(
      @PathVariable Long operatoreId,
      @RequestParam String azione,
      @RequestParam(required = false) String descrizione)
      throws Exception
  {
    service.registra(operatoreId, azione, descrizione);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }
}
