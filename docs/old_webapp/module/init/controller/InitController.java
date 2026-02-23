package dev.crm.module.init.controller;

import dev.crm.module.init.dto.WizardCompleteRequestDto;
import dev.crm.module.init.dto.WizardStatusDto;
import dev.crm.module.init.service.InitService;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/init")
public class InitController
{
  private final InitService initService;

  public InitController(InitService initService)
  {
    this.initService = initService;
  }

  @GetMapping("/status")
  public ApiPayload getStatus()
  {
    WizardStatusDto status;
    ApiPayload response;

    try {
      status = initService.getStatus();

      response = ApiResponse.create()
          .out(status)
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to get wizard status: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }

  @PostMapping("/complete")
  public ApiPayload completeWizard(
      @Valid @RequestBody WizardCompleteRequestDto request)
  {
    ApiPayload response;

    try {
      initService.completeWizard(request);

      response = ApiResponse.create()
          .out(Map.of("success", true, "message", "Wizard completato con successo"))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (IllegalStateException e) {
      response = ApiResponse.create()
          .err(true)
          .log(e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (IllegalArgumentException e) {
      response = ApiResponse.create()
          .err(true)
          .log(e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
          .err(true)
          .log("Failed to complete wizard: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }
}
