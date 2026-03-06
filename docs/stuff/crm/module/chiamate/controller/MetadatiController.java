package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.MetadatiDto;
import dev.crm.module.chiamate.service.MetadatiService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/metadati")
public class MetadatiController
{
  private final MetadatiService service;

  public MetadatiController(MetadatiService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload salva(@RequestBody MetadatiDto dto)
  {
    return ApiResponse.create().out(service.salva(dto)).status(200).build();
  }

  @GetMapping("/{chiamataId}")
  public ApiPayload getMetadati(@PathVariable Long chiamataId)
  {
    var result = service.getMetadati(chiamataId);
    if (result.isPresent()) {
      return ApiResponse.create().out(result.get()).build();
    }
    return ApiResponse.create()
        .err(true)
        .log("Metadati non trovati")
        .status(200)
        .build();
  }
}
