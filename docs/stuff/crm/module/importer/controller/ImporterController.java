package dev.crm.module.importer.controller;

import dev.crm.module.importer.dao.CampoDizionarioDao;
import dev.crm.module.importer.dto.CampoDizionarioDto;
import dev.crm.module.importer.dto.ColumnMappingDto;
import dev.crm.module.importer.dto.ImportResultDto;
import dev.crm.module.importer.dto.ImportSessionDto;
import dev.crm.module.importer.dto.ValidationResultDto;
import dev.crm.module.importer.service.ImporterService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.excel.ImportConfig;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/liste/import")
public class ImporterController
{
  private final ImporterService service;
  private final CampoDizionarioDao campoDizionarioDao;

  public ImporterController(ImporterService service, CampoDizionarioDao campoDizionarioDao)
  {
    this.service = service;
    this.campoDizionarioDao = campoDizionarioDao;
  }

  /** Ottiene lo storico delle importazioni */
  @GetMapping("/history")
  public ApiPayload
  getHistory(@RequestParam(defaultValue = "50") int limit,
             @RequestParam(defaultValue = "0") int offset) throws Exception
  {
    List<?> data;
    int total;
    Map<String, Object> paged;
    ApiPayload response;

    data = service.getImportHistory(limit, offset);
    total = service.getImportHistoryCount();
    paged = Map.of(
      "items", data,
      "offset", offset,
      "limit", limit,
      "total", total,
      "hasNext", (limit > 0) && ((offset + limit) < total));
    response = ApiResponse.create().out(paged).build();

    return response;
  }

  // ========================================================================
  // Gestione Campi Contatto (per mapping durante import)
  // ========================================================================

  /** Ottiene tutti i campi disponibili per il mapping */
  @GetMapping("/campi")
  public ApiPayload getCampiContatto() throws Exception
  {
    List<CampoDizionarioDto> campi;
    ApiPayload response;

    campi = campoDizionarioDao.findAll();
    response = ApiResponse.create().out(campi).build();

    return response;
  }

  /** Ottiene un campo specifico per nome */
  @GetMapping("/campi/{nomeCampo}")
  public ApiPayload
  getCampoContattoByNome(@PathVariable String nomeCampo) throws Exception
  {
    java.util.Optional<CampoDizionarioDto> result;
    ApiPayload response;

    result = campoDizionarioDao.findByNomeCampo(nomeCampo);
    if (result.isPresent()) {
      response = ApiResponse.create().out(result.get()).build();
      return response;
    }

    response = ApiResponse.create()
      .err(true)
      .log("Campo non trovato")
      .status(200)
      .build();

    return response;
  }

  /** Ottiene solo i campi obbligatori */
  @GetMapping("/campi/obbligatori")
  public ApiPayload getCampiObbligatori() throws Exception
  {
    List<CampoDizionarioDto> campi;
    ApiPayload response;

    campi = campoDizionarioDao.findObbligatori();
    response = ApiResponse.create().out(campi).build();

    return response;
  }

  // ========================================================================
  // Workflow Import
  // ========================================================================

  /** STEP 1: Upload e analisi del file Restituisce sessionId, headers, preview e row count */
  @PostMapping("/analyze")
  public ApiPayload
  analyzeUpload(@RequestParam("file") MultipartFile file)
  {
    ImportSessionDto created;
    ApiPayload response;

    if (file.isEmpty()) {
      response = ApiResponse.create()
        .err(true)
        .log("File vuoto")
        .status(200)
        .build();
      return response;
    }

    try {
      created = service.analyzeFile(file.getInputStream(), file.getOriginalFilename());
      response = ApiResponse.create().out(created).status(200).build();
      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
        .err(true)
        .log(e.getMessage())
        .status(200)
        .build();
      return response;
    }
  }

  /** STEP 2: Salva il mapping delle colonne scelto dall'utente */
  @PostMapping("/mapping")
  public ApiPayload
  saveMapping(@RequestBody ColumnMappingDto dto) throws Exception
  {
    Map<String, Object> result;
    ApiPayload response;

    service.saveColumnMapping(dto.sessionId, dto.columnMapping);
    result = Map.of("success", true);
    response = ApiResponse.create().out(result).build();

    return response;
  }

  /** STEP 3: Valida i dati prima dell'importazione */
  @PostMapping("/validate/{sessionId}")
  public ApiPayload
  validate(@PathVariable String sessionId) throws Exception
  {
    ValidationResultDto result;
    ApiPayload response;

    result = service.validateData(sessionId);
    response = ApiResponse.create().out(result).build();

    return response;
  }

  /** STEP 4: Esegue l'importazione finale */
  @PostMapping("/execute/{sessionId}")
  public ApiPayload
  executeImport(@PathVariable String sessionId,
                @RequestBody(required = false) ExecuteImportRequest request)
                throws Exception
  {
    Long listaId;
    String listaName;
    ImportResultDto result;
    ApiPayload response;

    listaId = request != null ? request.listaId : null;
    listaName = request != null ? request.listaName : null;
    result = service.executeImport(sessionId, listaId, listaName);
    response = ApiResponse.create().out(result).status(200).build();

    return response;
  }

  // DTO per request body
  public static class ExecuteImportRequest
  {
    public Long listaId; // ID lista esistente (opzionale)
    public String listaName; // Nome nuova lista (opzionale)
  }

  @PostMapping("/file")
  public ApiPayload
  importFile(@RequestParam("path") String path) throws Exception
  {
    ImportConfig config;
    ImportResultDto result;
    ApiPayload response;

    config = new ImportConfig();
    config.setNormalizationStrategy(new dev.springtools.util.excel.strategy.ContactNormalizationStrategy());
    result = service.importFromFile(path, config);
    response = ApiResponse.create().out(result).status(200).build();

    return response;
  }
}
