package dev.crm.module.agenti.controller;

import dev.crm.module.agenti.entity.SlotDisponibileEntity;
import dev.crm.module.agenti.service.SlotDisponibileService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import dev.springtools.util.Log;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenti")
public class SlotController
{
  private static final Log log = Log.get(SlotController.class);
  private final SlotDisponibileService service;

  public SlotController(SlotDisponibileService service)
  {
    this.service = service;
  }

  /**
   * GET del prossimo slot disponibile per un agente
   * @param agenteId ID agente mappato sul path dello URL
   * @param durataMinuti (Opzionale) Durata slot in minuti (default: 30)
   */
  @GetMapping("/{agenteId}/slot")
  public ApiPayload
  findProssimoSlot(@PathVariable Long agenteId,
                   @RequestParam(required = false, defaultValue = "30") Integer durataMinuti)
  {
    ApiPayload payload;
    try {
      SlotDisponibileEntity result;
      result = service.findProssimoSlot(agenteId, durataMinuti);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Nessuno slot disponibile").out(null)
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
   * GET del prossimo slot disponibile tra tutti gli agenti attivi
   * @param durataMinuti (Opzionale) Durata slot in minuti (default: 30)
   */
  @GetMapping("/slot")
  public ApiPayload
  findProssimoSlotTuttiAgenti(@RequestParam(required = false, defaultValue = "30") Integer durataMinuti)
  {
    ApiPayload payload;
    try {
      SlotDisponibileEntity result;
      result = service.findProssimoSlotTuttiAgenti(durataMinuti);
      if (result != null) {
        payload = ApiResponse.create()
            .err(false).log(null).out(result)
            .status(200).contentType("application/json")
            .build();
      } else {
        payload = ApiResponse.create()
            .err(true).log("Nessuno slot disponibile").out(null)
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
   * GET di tutti gli slot disponibili per un agente in un range di date
   * @param agenteId ID agente mappato sul path dello URL
   * @param dataInizio Data inizio range (ISO date)
   * @param dataFine Data fine range (ISO date)
   * @param durataMinuti (Opzionale) Durata slot in minuti (default: 30)
   */
  @GetMapping("/{agenteId}/slots")
  public ApiPayload
  findSlotDisponibili(@PathVariable Long agenteId,
                      @RequestParam String dataInizio,
                      @RequestParam String dataFine,
                      @RequestParam(required = false, defaultValue = "30") Integer durataMinuti)
  {
    ApiPayload payload;
    try {
      LocalDate inizio;
      LocalDate fine;
      List<SlotDisponibileEntity> result;
      inizio = LocalDate.parse(dataInizio);
      fine = LocalDate.parse(dataFine);
      result = service.findSlotDisponibili(agenteId, inizio, fine, durataMinuti);
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
