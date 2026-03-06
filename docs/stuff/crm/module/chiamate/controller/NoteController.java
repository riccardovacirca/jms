package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dto.NoteChiamataDto;
import dev.crm.module.chiamate.service.NoteService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/note")
public class NoteController
{
  private final NoteService service;

  public NoteController(NoteService service)
  {
    this.service = service;
  }

  @PostMapping
  public ApiPayload salva(@RequestBody NoteChiamataDto dto)
  {
    NoteChiamataDto created = service.salva(dto);
    return ApiResponse.create().out(created).status(200).build();
  }

  @GetMapping("/{chiamataId}")
  public ApiPayload getNote(@PathVariable Long chiamataId)
  {
    List<NoteChiamataDto> data = service.getNote(chiamataId);
    return ApiResponse.create().out(data).build();
  }
}
