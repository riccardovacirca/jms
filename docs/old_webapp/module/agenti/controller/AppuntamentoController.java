package dev.crm.module.agenti.controller;

import dev.crm.module.agenti.entity.AppuntamentoEntity;
import dev.crm.module.agenti.service.AppuntamentoService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenti")
public class AppuntamentoController
{
  private static final Log log = Log.get(AppuntamentoController.class);
  private final AppuntamentoService service;

  public AppuntamentoController(AppuntamentoService service)
  {
    this.service = service;
  }

  /**
   * GET di un elenco di tutti gli appuntamenti, opzionalmente paginato
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   */
  @GetMapping("/appuntamenti")
  public ApiPayload
  findAll(@RequestParam(required = false) Integer limit,
          @RequestParam(required = false) Integer offset)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findAll(limit, offset);
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
   * GET di un elenco di appuntamenti per agente, opzionalmente paginato e filtrato per date
   * @param agenteId ID agente mappato sul path dello URL
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   * @param dataInizio (opzionale) filtro data inizio (ISO datetime)
   * @param dataFine (opzionale) filtro data fine (ISO datetime)
   */
  @GetMapping("/{agenteId}/appuntamenti")
  public ApiPayload
  findByAgente(@PathVariable Long agenteId,
               @RequestParam(required = false) Integer limit,
               @RequestParam(required = false) Integer offset,
               @RequestParam(required = false) String dataInizio,
               @RequestParam(required = false) String dataFine)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findByAgente(agenteId, limit, offset, dataInizio, dataFine);
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
   * GET di un appuntamento in base al parametro ID
   * @param id ID appuntamento mappato sul path dello URL
   */
  @GetMapping("/appuntamenti/{id}")
  public ApiPayload findById(@PathVariable Long id)
  {
    ApiPayload payload;
    try {
      AppuntamentoEntity result;
      result = service.findById(id);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Appuntamento non trovato").out(null)
            .status(200).contentType("application/json")
            .build();
      }
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
   * CREATE di un appuntamento
   * @param entity dati dell'appuntamento mappati sul body della request
   */
  @PostMapping("/appuntamenti")
  public ApiPayload create(@RequestBody AppuntamentoEntity entity)
  {
    ApiPayload payload;
    try {
      AppuntamentoEntity result;
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
   * UPDATE di un appuntamento in base al parametro ID
   * @param id ID appuntamento mappato sul path dello URL
   * @param entity dati aggiornati dell'appuntamento mappati sul body della request
   */
  @PutMapping("/appuntamenti/{id}")
  public ApiPayload update(@PathVariable Long id,
                           @RequestBody AppuntamentoEntity entity)
  {
    ApiPayload payload;
    try {
      AppuntamentoEntity result;
      entity.id = id;
      result = service.update(entity);
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
   * DELETE di un appuntamento in base al parametro ID
   * @param id ID appuntamento mappato sul path dello URL
   */
  @DeleteMapping("/appuntamenti/{id}")
  public ApiPayload delete(@PathVariable Long id)
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
