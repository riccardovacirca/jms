package dev.crm.module.sedi.dto;

import java.time.LocalDateTime;

public class SedeOperatoreDto
{
  public Long id;
  public Long sedeId;
  public Long operatoreId;
  public LocalDateTime createdAt;

  public SedeOperatoreDto()
  {
  }

  public SedeOperatoreDto(Long id, Long sedeId, Long operatoreId, LocalDateTime createdAt)
  {
    this.id = id;
    this.sedeId = sedeId;
    this.operatoreId = operatoreId;
    this.createdAt = createdAt;
  }
}
