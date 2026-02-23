package dev.crm.module.init.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfigurazioneDto
{
  public Long id;

  @NotBlank
  public String categoria;

  @NotBlank
  public String chiave;

  public String valore;
  public String tipo;
  public String descrizione;
  public Integer modificabile;

  public ConfigurazioneDto()
  {
  }
}
