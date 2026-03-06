package dev.crm.module.logs.dto;

import jakarta.validation.constraints.NotBlank;

public class LogDto
{
  public Long id;

  public String timestamp;

  @NotBlank
  public String level;  // DEBUG, INFO, WARN, ERROR

  @NotBlank
  public String module;  // Nome modulo

  @NotBlank
  public String message;  // Messaggio log

  public String data;  // Dati JSON opzionali

  public Long userId;
  public String sessionId;
  public String ipAddress;
  public String userAgent;
  public String createdAt;

  public LogDto()
  {
  }
}
