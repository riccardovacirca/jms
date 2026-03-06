package dev.crm.module.agenti.controller;

import dev.crm.module.agenti.entity.AgenteEntity;
import dev.crm.module.agenti.service.AgenteService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenti")
public class AgenteController
{
  private static final Log log = Log.get(AgenteController.class);
  private final AgenteService service;

  public AgenteController(AgenteService service)
  {
    this.service = service;
  }

  /**
   * GET di un elenco di agenti, opzionalmente paginato e filtrato per stato attivo
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   * @param attivo (opzionale) filtra per stato attivo
   */
  @GetMapping
  public ApiPayload
  findAll(@RequestParam(required = false) Integer limit,
          @RequestParam(required = false) Integer offset,
          @RequestParam(required = false) Integer attivo)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findAll(limit, offset, attivo);
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
   * GET di un agente in base al parametro ID
   * @param id ID agente mappato sul path dello URL
   */
  @GetMapping("/{id}")
  public ApiPayload findById(@PathVariable Long id)
  {
    ApiPayload payload;
    try {
      AgenteEntity result;
      result = service.findById(id);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Agente non trovato").out(null)
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
   * CREATE di un agente
   * @param entity dati dell'agente mappati sul body della request
   */
  @PostMapping
  public ApiPayload create(@RequestBody AgenteEntity entity)
  {
    ApiPayload payload;
    try {
      AgenteEntity result;
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
   * UPDATE di un agente in base al parametro ID
   * @param id ID agente mappato sul path dello URL
   * @param entity dati aggiornati dell'agente mappati sul body della request
   */
  @PutMapping("/{id}")
  public ApiPayload update(@PathVariable Long id, @RequestBody AgenteEntity entity)
  {
    ApiPayload payload;
    try {
      AgenteEntity result;
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
   * DELETE di un agente in base al parametro ID
   * @param id ID agente mappato sul path dello URL
   */
  @DeleteMapping("/{id}")
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
