package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.RichiamoDto;
import dev.crm.module.chiamate.service.RichiamoService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/richiami")
public class RichiamoController
{

  private final RichiamoService service;

  public RichiamoController(RichiamoService service)
  {
    this.service = service;
  }

  @GetMapping
  public ApiPayload findAll(
      @RequestParam(required = false) Long operatoreId,
      @RequestParam(required = false) String stato)
      throws Exception
  {
    List<RichiamoDto> data;
    if (operatoreId != null && stato != null) {
      data = service.findByOperatoreIdAndStato(operatoreId, stato);
    } else if (operatoreId != null) {
      data = service.findByOperatoreId(operatoreId);
    } else {
      data = service.findAll();
    }
    return ApiResponse.create().out(data).build();
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
        .log("Richiamo non trovato")
        .status(200)
        .build();
  }

  @GetMapping("/imminenti/{operatoreId}")
  public ApiPayload findImminenti(@PathVariable Long operatoreId)
      throws Exception
  {
    List<RichiamoDto> data = service.findImminenti(operatoreId);
    return ApiResponse.create().out(data).build();
  }

  @PostMapping
  public ApiPayload create(@RequestBody RichiamoDto dto) throws Exception
  {
    RichiamoDto created = service.create(dto);
    return ApiResponse.create().out(created).status(200).build();
  }

  @PutMapping("/{id}")
  public ApiPayload update(@PathVariable Long id, @RequestBody RichiamoDto dto)
      throws Exception
  {
    dto.id = id;
    RichiamoDto updated = service.update(dto);
    return ApiResponse.create().out(updated).build();
  }

  @DeleteMapping("/{id}")
  public ApiPayload delete(@PathVariable Long id) throws Exception
  {
    service.delete(id);
    return ApiResponse.create().out(Map.of("success", true)).status(200).build();
  }

  @PutMapping("/{id}/posticipa")
  public ApiPayload posticipa(@PathVariable Long id, @RequestParam String nuovaDataOra)
      throws Exception
  {
    LocalDateTime dataOra = LocalDateTime.parse(nuovaDataOra);
    RichiamoDto updated = service.posticipa(id, dataOra);
    return ApiResponse.create().out(updated).build();
  }

  @PutMapping("/{id}/annulla")
  public ApiPayload annulla(@PathVariable Long id,
      @RequestParam(required = false) String motivo)
      throws Exception
  {
    RichiamoDto updated = service.annulla(id, motivo);
    return ApiResponse.create().out(updated).build();
  }

  @PutMapping("/{id}/completa")
  public ApiPayload completa(@PathVariable Long id) throws Exception
  {
    RichiamoDto updated = service.completa(id);
    return ApiResponse.create().out(updated).build();
  }
}
