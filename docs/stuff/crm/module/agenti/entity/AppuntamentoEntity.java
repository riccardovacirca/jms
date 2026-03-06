package dev.crm.module.agenti.entity;

import java.time.LocalDateTime;

public class AppuntamentoEntity
{
  public Long id;
  public Long agenteId;
  public Long contattoId;
  public LocalDateTime dataOra;
  public Integer durataMinuti;
  public String note;
  public String stato;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public AppuntamentoEntity()
  {
  }

  public AppuntamentoEntity(
      Long id,
      Long agenteId,
      Long contattoId,
      LocalDateTime dataOra,
      Integer durataMinuti,
      String note,
      String stato,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.agenteId = agenteId;
    this.contattoId = contattoId;
    this.dataOra = dataOra;
    this.durataMinuti = durataMinuti;
    this.note = note;
    this.stato = stato;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
