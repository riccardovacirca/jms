package dev.crm.module.agenti.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DisponibilitaEntity
{
  public Long id;
  public Long agenteId;
  public Integer giornoSettimana;
  public LocalTime oraInizio;
  public LocalTime oraFine;
  public LocalDateTime createdAt;

  public DisponibilitaEntity()
  {
  }

  public DisponibilitaEntity(
      Long id,
      Long agenteId,
      Integer giornoSettimana,
      LocalTime oraInizio,
      LocalTime oraFine,
      LocalDateTime createdAt)
  {
    this.id = id;
    this.agenteId = agenteId;
    this.giornoSettimana = giornoSettimana;
    this.oraInizio = oraInizio;
    this.oraFine = oraFine;
    this.createdAt = createdAt;
  }
}
