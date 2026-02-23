package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.EccezioniChiamataDto;
import dev.crm.module.chiamate.service.EccezioniChiamataService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/errori")
public class EccezioniChiamataController
{
  private final EccezioniChiamataService service;

  public EccezioniChiamataController(EccezioniChiamataService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload registra(@RequestBody EccezioniChiamataDto dto)
  {
    EccezioniChiamataDto created = service.registra(dto);
    return ApiResponse.create().out(created).status(200).build();
  }

  @GetMapping
  public ApiPayload all()
  {
    List<EccezioniChiamataDto> data = service.all();
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/{chiamataId}")
  public ApiPayload byChiamata(@PathVariable Long chiamataId)
  {
    List<EccezioniChiamataDto> data = service.byChiamata(chiamataId);
    return ApiResponse.create().out(data).build();
  }
}
