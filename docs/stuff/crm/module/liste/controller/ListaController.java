package dev.crm.module.liste.controller;

import dev.crm.module.liste.dto.ListaDto;
import dev.crm.module.liste.service.ListaService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/liste")
public class ListaController
{
  private final ListaService service;

  public ListaController(ListaService service)
  {
    this.service = service;
  }

  @GetMapping
  public ApiPayload
  findAll(@RequestParam(defaultValue = "50") int limit,
          @RequestParam(defaultValue = "0") int offset,
          @RequestParam(required = false) String q) throws Exception
  {
    Object data;
    int total;
    Map<String, Object> result;
    ApiPayload response;

    if (q != null && !q.trim().isEmpty()) {
      data = service.search(q.trim(), limit, offset);
      total = service.countSearch(q.trim());
    } else {
      data = service.findAll(limit, offset);
      total = service.count();
    }

    result = Map.of(
      "items", data,
      "offset", offset,
      "limit", limit,
      "total", total,
      "hasNext", (limit > 0) && ((offset + limit) < total),
      "query", q != null ? q : "");
    
    response = ApiResponse
      .create()
      .out(result)
      .contentType("application/json")
      .build();

    return response;
  }

  @GetMapping("/{id}")
  public ApiPayload
  findById(@PathVariable Long id) throws Exception
  {
    java.util.Optional<ListaDto> lista;
    ApiPayload response;

    lista = service.findById(id);
    if (lista.isPresent()) {
      response = ApiResponse
        .create()
        .out(lista.get())
        .contentType("application/json")
        .build();
      
      return response;
    }

    response = ApiResponse.create()
      .err(true)
      .log("Lista non trovata")
      .status(200)
      .contentType("application/json")
      .build();

    return response;
  }

  @PostMapping
  public ApiPayload
  create(@RequestBody ListaDto dto) throws Exception
  {
    ListaDto created;
    ApiPayload response;

    created = service.create(dto);
    response = ApiResponse
      .create()
      .out(created)
      .status(200)
      .contentType("application/json")
      .build();

    return response;
  }

  @PutMapping("/{id}")
  public ApiPayload
  update(@PathVariable Long id,
         @RequestBody ListaDto dto) throws Exception
  {
    ListaDto updated;
    ApiPayload response;

    dto.id = id;
    updated = service.update(dto);
    response = ApiResponse
      .create()
      .out(updated)
      .contentType("application/json")
      .build();

    return response;
  }

  @DeleteMapping("/{id}")
  public ApiPayload
  delete(@PathVariable Long id) throws Exception
  {
    Map<String, Object> result;
    ApiPayload response;

    service.delete(id);
    result = Map.of("success", true);
    response = ApiResponse
      .create()
      .out(result)
      .status(200)
      .contentType("application/json")
      .build();

    return response;
  }

  @PutMapping("/{id}/stato")
  public ApiPayload
  updateStato(@PathVariable Long id,
              @RequestParam Integer stato) throws Exception
  {
    ListaDto updated;
    ApiPayload response;

    updated = service.updateStato(id, stato);
    response = ApiResponse
      .create()
      .out(updated)
      .contentType("application/json")
      .build();

    return response;
  }

  @PutMapping("/{id}/scadenza")
  public ApiPayload
  updateScadenza(@PathVariable Long id,
                 @RequestParam String scadenza) throws Exception
  {
    LocalDate scadenzaDate;
    ListaDto updated;
    ApiPayload response;

    scadenzaDate = LocalDate.parse(scadenza);
    updated = service.updateScadenza(id, scadenzaDate);
    response = ApiResponse
      .create()
      .out(updated)
      .contentType("application/json")
      .build();

    return response;
  }

  @GetMapping("/{listaId}/contatti")
  public ApiPayload
  findContatti(@PathVariable Long listaId) throws Exception
  {
    Object contatti;
    ApiPayload response;

    contatti = service.findContattiByListaId(listaId);
    response = ApiResponse
      .create()
      .out(contatti)
      .contentType("application/json")
      .build();

    return response;
  }

  @PostMapping("/{listaId}/contatti/{contattoId}")
  public ApiPayload
  addContatto(@PathVariable Long listaId,
              @PathVariable Long contattoId) throws Exception
  {
    Map<String, Object> result;
    ApiPayload response;

    service.addContatto(listaId, contattoId);
    result = Map.of("success", true);
    response = ApiResponse
      .create()
      .out(result)
      .contentType("application/json")
      .build();

    return response;
  }

  @DeleteMapping("/{listaId}/contatti/{contattoId}")
  public ApiPayload
  removeContatto(@PathVariable Long listaId,
                 @PathVariable Long contattoId) throws Exception
  {
    Map<String, Object> result;
    ApiPayload response;

    service.removeContatto(listaId, contattoId);
    result = Map.of("success", true);
    response = ApiResponse
      .create()
      .out(result)
      .status(200)
      .contentType("application/json")
      .build();

    return response;
  }
}
