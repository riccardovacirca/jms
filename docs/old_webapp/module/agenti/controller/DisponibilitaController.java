package dev.crm.module.agenti.controller;

import dev.crm.module.agenti.entity.DisponibilitaEntity;
import dev.crm.module.agenti.service.DisponibilitaService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenti")
public class DisponibilitaController
{
  private static final Log log = Log.get(DisponibilitaController.class);
  private final DisponibilitaService service;

  public DisponibilitaController(DisponibilitaService service)
  {
    this.service = service;
  }

  /**
   * GET di un elenco delle disponibilità di un agente, opzionalmente paginato
   * @param agenteId ID agente mappato sul path dello URL
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   */
  @GetMapping("/{agenteId}/disponibilita")
  public ApiPayload
  findByAgente(@PathVariable Long agenteId,
               @RequestParam(required = false) Integer limit,
               @RequestParam(required = false) Integer offset)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findByAgente(agenteId, limit, offset);
      payload = ApiResponse.create()
          .err(false).log(null).out(result)
          .status(200).contentType("application/json")
          .build();
    } catch (Throwable e) {
      log.error(e);
      payload = ApiResponse.create()
          .err(true).log(Log.getSafeErrorMessage(e)).out(null)
          .status(200).contentType("application/json")
          .build();
    }
    return payload;
  }

  /**
   * CREATE di una disponibilità per un agente
   * @param agenteId ID agente mappato sul path dello URL
   * @param entity dati della disponibilità mappati sul body della request
   */
  @PostMapping("/{agenteId}/disponibilita")
  public ApiPayload
  createDisponibilita(@PathVariable Long agenteId,
                      @RequestBody DisponibilitaEntity entity)
  {
    ApiPayload payload;
    try {
      DisponibilitaEntity result;
      entity.agenteId = agenteId;
      result = service.create(entity);
      payload = ApiResponse.create()
          .err(false).log(null).out(result)
          .status(200).contentType("application/json")
          .build();
    } catch (Throwable e) {
      log.error(e);
      payload = ApiResponse.create()
          .err(true).log(Log.getSafeErrorMessage(e)).out(null)
          .status(200).contentType("application/json")
          .build();
    }
    return payload;
  }

  /**
   * DELETE di una disponibilità in base al parametro ID
   * @param id ID disponibilità mappato sul path dello URL
   */
  @DeleteMapping("/disponibilita/{id}")
  public ApiPayload deleteDisponibilita(@PathVariable Long id)
  {
    ApiPayload payload;
    try {
      service.delete(id);
      payload = ApiResponse.create()
          .err(false).log(null).out(null)
          .status(200).contentType("application/json")
          .build();
    } catch (Throwable e) {
      log.error(e);
      payload = ApiResponse.create()
          .err(true).log(Log.getSafeErrorMessage(e)).out(null)
          .status(200).contentType("application/json")
          .build();
    }
    return payload;
  }
}
