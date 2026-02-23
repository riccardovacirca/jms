package dev.crm.module.init.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class WizardCompleteRequestDto
{
  @Valid
  @NotNull
  public AziendaDto azienda;

  @Valid
  @NotNull
  public OwnerAccountDto ownerAccount;

  @Valid
  public List<SedeDto> sedi;

  @Valid
  public List<OwnerAccountDto> adminAccounts;

  public List<ConfigurazioneDto> configurazioni;

  public WizardCompleteRequestDto()
  {
  }
}
