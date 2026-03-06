package dev.crm.module.operatori.controller;

import dev.crm.module.operatori.service.CampagnaAssegnataService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operatori")
public class CampagnaAssegnataController
{

  private final CampagnaAssegnataService service;

  public CampagnaAssegnataController(CampagnaAssegnataService service)
  {
    this.service = service;
  }

  @GetMapping("/{operatoreId}/campagne")
  public ApiPayload findCampagne(
      @PathVariable Long operatoreId, @RequestParam(required = false) Boolean dettagli)
      throws Exception
  {
    ApiPayload response;

    if (Boolean.TRUE.equals(dettagli)) {
      // Restituisce i dettagli completi delle campagne
      response = ApiResponse
          .create()
          .out(service.findCampagneByOperatoreId(operatoreId))
          .status(200)
          .contentType("application/json")
          .build();
    } else {
      // Restituisce solo le associazioni (retrocompatibilità)
      response = ApiResponse
          .create()
          .out(service.findByOperatoreId(operatoreId))
          .status(200)
          .contentType("application/json")
          .build();
    }

    return response;
  }

  @PostMapping("/{operatoreId}/campagne/{campagnaId}")
  public ApiPayload assegnaCampagna(
      @PathVariable Long operatoreId, @PathVariable Long campagnaId) throws Exception
  {
    service.assegna(operatoreId, campagnaId);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }

  @DeleteMapping("/{operatoreId}/campagne/{campagnaId}")
  public ApiPayload rimuoviCampagna(
      @PathVariable Long operatoreId, @PathVariable Long campagnaId) throws Exception
  {
    service.rimuovi(operatoreId, campagnaId);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }
}
