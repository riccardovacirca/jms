package dev.crm.module.operatori.dto;

import java.time.LocalDateTime;

public class StatoOperatoreDto
{
  public Long id;
  public Long operatoreId;
  public String stato;
  public LocalDateTime timestamp;

  public StatoOperatoreDto()
  {
  }

  public StatoOperatoreDto(Long id, Long operatoreId, String stato, LocalDateTime timestamp)
  {
    this.id = id;
    this.operatoreId = operatoreId;
    this.stato = stato;
    this.timestamp = timestamp;
  }
}
