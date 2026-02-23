package dev.crm.module.chiamate.dto;

import java.time.LocalDateTime;

public class RichiamoDto
{
  public Long id;
  public Long operatoreId;
  public Long contattoId;
  public LocalDateTime dataOra;
  public Integer durataMinuti;
  public String note;
  public String stato;
  public Long campagnaId;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public RichiamoDto()
  {
  }

  public RichiamoDto(
      Long id,
      Long operatoreId,
      Long contattoId,
      LocalDateTime dataOra,
      Integer durataMinuti,
      String note,
      String stato,
      Long campagnaId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.operatoreId = operatoreId;
    this.contattoId = contattoId;
    this.dataOra = dataOra;
    this.durataMinuti = durataMinuti;
    this.note = note;
    this.stato = stato;
    this.campagnaId = campagnaId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
