package dev.crm.module.sedi.dto;

import java.time.LocalDateTime;

public class SedeCampagnaDto
{
  public Long id;
  public Long sedeId;
  public Long campagnaId;
  public LocalDateTime createdAt;

  public SedeCampagnaDto()
  {
  }

  public SedeCampagnaDto(Long id, Long sedeId, Long campagnaId, LocalDateTime createdAt)
  {
    this.id = id;
    this.sedeId = sedeId;
    this.campagnaId = campagnaId;
    this.createdAt = createdAt;
  }
}
