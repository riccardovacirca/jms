package dev.crm.module.chiamate.dto;

import java.time.LocalDateTime;

public class PromemoriaDto
{
  public Long id;
  public Long richiamoId;
  public Integer minutiAnticipo;
  public LocalDateTime triggerAt;
  public Integer inviato;
  public LocalDateTime createdAt;

  public PromemoriaDto()
  {
  }

  public PromemoriaDto(
      Long id,
      Long richiamoId,
      Integer minutiAnticipo,
      LocalDateTime triggerAt,
      Integer inviato,
      LocalDateTime createdAt)
  {
    this.id = id;
    this.richiamoId = richiamoId;
    this.minutiAnticipo = minutiAnticipo;
    this.triggerAt = triggerAt;
    this.inviato = inviato;
    this.createdAt = createdAt;
  }
}
