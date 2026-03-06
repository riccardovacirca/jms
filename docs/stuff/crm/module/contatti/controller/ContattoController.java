package dev.crm.module.contatti.controller;

import dev.crm.module.contatti.entity.ContattoEntity;
import dev.crm.module.contatti.service.ContattoService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/liste/contatti")
public class ContattoController
{
  private static final Log log = Log.get(ContattoController.class);
  private final ContattoService service;

  public ContattoController(ContattoService service)
  {
    this.service = service;
  }

  /**
   * GET di un elenco di contatti, opzionalmente paginato e filtrato per lista
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   * @param listaId (opzionale) ID lista per filtrare i contatti
   */
  @GetMapping
  public ApiPayload
  findAll(@RequestParam(required = false) Integer limit,
          @RequestParam(required = false) Integer offset,
          @RequestParam(required = false) Long listaId)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findAll(limit, offset, listaId);
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
   * GET di un contatto in base al parametro ID
   * @param id ID contatto mappato sul path dello URL
   */
  @GetMapping("/{id}")
  public ApiPayload findById(@PathVariable Long id)
  {
    ApiPayload payload;
    try {
      ContattoEntity result;
      result = service.findById(id);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Contatto non trovato").out(null)
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
   * GET dei contatti corrispondenti a una query di ricerca
   * @param q testo da cercare
   * @param limit numero massimo di risultati (default: 20)
   */
  @GetMapping("/search")
  public ApiPayload
  search(@RequestParam String q,
         @RequestParam(defaultValue = "20") int limit)
  {
    ApiPayload payload;
    try {
      List<ContattoEntity> result;
      result = service.search(q, limit);
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
   * CREATE di un contatto
   * @param entity dati del contatto mappati sul body della request
   */
  @PostMapping
  public ApiPayload create(@RequestBody ContattoEntity entity)
  {
    ApiPayload payload;
    try {
      ContattoEntity result;
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
   * UPDATE di un contatto in base al parametro ID
   * @param id ID contatto mappato sul path dello URL
   * @param entity dati del contatto mappati sul body della request
   */
  @PutMapping("/{id}")
  public ApiPayload update(@PathVariable Long id,
                           @RequestBody ContattoEntity entity)
  {
    ApiPayload payload;
    try {
      ContattoEntity result;
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
   * DELETE di un contatto in base al parametro ID
   * @param id ID contatto mappato sul path dello URL
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

  /**
   * UPDATE dello stato di un contatto in base al parametro ID
   * @param id ID contatto mappato sul path dello URL
   * @param stato nuovo stato da impostare
   */
  @PutMapping("/{id}/stato")
  public ApiPayload updateStato(@PathVariable Long id,
                                @RequestParam Integer stato)
  {
    ApiPayload payload;
    try {
      ContattoEntity result;
      result = service.updateStato(id, stato);
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
   * UPDATE del flag blacklist di un contatto in base al parametro ID
   * @param id ID contatto mappato sul path dello URL
   * @param value valore blacklist da impostare
   */
  @PutMapping("/{id}/blacklist")
  public ApiPayload setBlacklist(@PathVariable Long id,
                                 @RequestParam Boolean value)
  {
    ApiPayload payload;
    try {
      ContattoEntity result;
      result = service.setBlacklist(id, value);
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
}
