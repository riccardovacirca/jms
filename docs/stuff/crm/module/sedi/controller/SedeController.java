package dev.crm.module.sedi.controller;

import dev.crm.module.sedi.dto.SedeDto;
import dev.crm.module.sedi.service.SedeService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sedi")
public class SedeController
{

  private final SedeService service;

  public SedeController(SedeService service)
  {
    this.service = service;
  }

  // CRUD sedi
  @GetMapping
  public ApiPayload findAll() throws Exception
  {
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
        .log("Sede non trovata")
        .status(200)
        .build();
  }

  @PostMapping
  public ApiPayload create(@RequestBody SedeDto dto) throws Exception
  {
    return ApiResponse.create().out(service.create(dto)).status(200).build();
  }

  @PutMapping
  public ApiPayload update(@RequestBody SedeDto dto) throws Exception
  {
    return ApiResponse.create().out(service.update(dto)).build();
  }

  @DeleteMapping("/{id}")
  public ApiPayload delete(@PathVariable Long id) throws Exception
  {
    service.delete(id);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }

  // Gestione operatori
  @GetMapping("/{sedeId}/operatori")
  public ApiPayload findOperatori(@PathVariable Long sedeId)
      throws Exception
  {
    return ApiResponse.create().out(service.findOperatoriBySedeId(sedeId)).build();
  }

  @PostMapping("/operatori")
  public ApiPayload associaOperatore(@RequestBody Map<String, Long> body)
      throws Exception
  {
    Long sedeId = body.get("sedeId");
    Long operatoreId = body.get("operatoreId");
    service.associaOperatore(sedeId, operatoreId);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }

  @DeleteMapping("/operatori")
  public ApiPayload rimuoviOperatore(@RequestBody Map<String, Long> body)
      throws Exception
  {
    Long sedeId = body.get("sedeId");
    Long operatoreId = body.get("operatoreId");
    service.rimuoviOperatore(sedeId, operatoreId);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }

  // Gestione campagne
  @GetMapping("/{sedeId}/campagne")
  public ApiPayload findCampagne(@PathVariable Long sedeId)
      throws Exception
  {
    return ApiResponse.create().out(service.findCampagneBySedeId(sedeId)).build();
  }

  @PostMapping("/campagne")
  public ApiPayload associaCampagna(@RequestBody Map<String, Long> body)
      throws Exception
  {
    Long sedeId = body.get("sedeId");
    Long campagnaId = body.get("campagnaId");
    service.associaCampagna(sedeId, campagnaId);
    return ApiResponse.create().out(Map.of("success", true)).build();
  }

  @DeleteMapping("/campagne")
  public ApiPayload rimuoviCampagna(@RequestBody Map<String, Long> body)
      throws Exception
  {
    Long sedeId = body.get("sedeId");
    Long campagnaId = body.get("campagnaId");
    service.rimuoviCampagna(sedeId, campagnaId);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }
}
