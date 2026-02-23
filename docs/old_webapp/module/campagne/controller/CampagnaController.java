package dev.crm.module.campagne.controller;

import dev.crm.module.campagne.entity.CampagnaEntity;
import dev.crm.module.campagne.service.CampagnaService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campagne")
public class CampagnaController
{
  private static final Log log = Log.get(CampagnaController.class);
  private final CampagnaService service;

  public CampagnaController(CampagnaService service)
  {
    this.service = service;
  }

  /**
   * GET di un elenco di tutte le campagne, opzionalmente paginato
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   */
  @GetMapping
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
   * GET di una campagna in base al parametro ID
   * @param id ID campagna mappato sul path dello URL
   */
  @GetMapping("/{id}")
  public ApiPayload findById(@PathVariable Long id)
  {
    ApiPayload payload;
    try {
      CampagnaEntity result;
      result = service.findById(id);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Campagna non trovata").out(null)
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
   * CREATE di una campagna
   * @param entity dati della campagna mappati sul body della request
   */
  @PostMapping
  public ApiPayload create(@RequestBody CampagnaEntity entity)
  {
    ApiPayload payload;
    try {
      CampagnaEntity result;
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
   * UPDATE di una campagna in base al parametro ID
   * @param id ID campagna mappato sul path dello URL
   * @param entity dati aggiornati della campagna mappati sul body della request
   */
  @PutMapping("/{id}")
  public ApiPayload update(@PathVariable Long id, @RequestBody CampagnaEntity entity)
  {
    ApiPayload payload;
    try {
      CampagnaEntity result;
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
   * DELETE di una campagna in base al parametro ID
   * @param id ID campagna mappato sul path dello URL
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
   * UPDATE dello stato di una campagna in base al parametro ID
   * @param id ID campagna mappato sul path dello URL
   * @param stato nuovo stato da impostare
   */
  @PutMapping("/{id}/stato")
  public ApiPayload updateStato(@PathVariable Long id, @RequestParam Integer stato)
  {
    ApiPayload payload;
    try {
      CampagnaEntity result;
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
   * GET di un elenco delle liste associate a una campagna, opzionalmente paginato
   * @param campagnaId ID campagna mappato sul path dello URL
   * @param limit (opzionale) numero massimo di risultati
   * @param offset (opzionale) offset di paginazione
   */
  @GetMapping("/{campagnaId}/liste")
  public ApiPayload
  findListe(@PathVariable Long campagnaId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset)
  {
    ApiPayload payload;
    try {
      Map<String, Object> result;
      result = service.findListe(campagnaId, limit, offset);
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
   * CREATE di una lista in una campagna
   * @param campagnaId ID campagna mappato sul path dello URL
   * @param listaId ID lista mappato sul path dello URL
   */
  @PostMapping("/{campagnaId}/liste/{listaId}")
  public ApiPayload addLista(@PathVariable Long campagnaId, @PathVariable Long listaId)
  {
    ApiPayload payload;
    try {
      service.addLista(campagnaId, listaId);
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
   * DELETE di una lista da una campagna
   * @param campagnaId ID campagna mappato sul path dello URL
   * @param listaId ID lista mappato sul path dello URL
   */
  @DeleteMapping("/{campagnaId}/liste/{listaId}")
  public ApiPayload removeLista(@PathVariable Long campagnaId, @PathVariable Long listaId)
  {
    ApiPayload payload;
    try {
      service.removeLista(campagnaId, listaId);
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
