package dev.crm.module.logs.dto;

import jakarta.validation.constraints.NotBlank;

public class LogCreateRequestDto
{
  @NotBlank
  public String level;  // DEBUG, INFO, WARN, ERROR

  @NotBlank
  public String module;  // Nome modulo

  @NotBlank
  public String message;  // Messaggio log

  public String data;  // Dati JSON opzionali

  public LogCreateRequestDto()
  {
  }
}
