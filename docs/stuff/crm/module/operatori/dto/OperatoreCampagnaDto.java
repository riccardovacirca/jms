package dev.crm.module.operatori.dto;

import java.time.LocalDateTime;

public class OperatoreCampagnaDto
{
  public Long id;
  public Long operatoreId;
  public Long campagnaId;
  public LocalDateTime createdAt;

  public OperatoreCampagnaDto()
  {
  }

  public OperatoreCampagnaDto(Long id, Long operatoreId, Long campagnaId, LocalDateTime createdAt)
  {
    this.id = id;
    this.operatoreId = operatoreId;
    this.campagnaId = campagnaId;
    this.createdAt = createdAt;
  }
}
