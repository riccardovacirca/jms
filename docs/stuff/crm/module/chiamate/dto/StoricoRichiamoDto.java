package dev.crm.module.chiamate.dto;

import java.time.LocalDateTime;

public class StoricoRichiamoDto
{
  public Long id;
  public Long richiamoId;
  public Long operatoreId;
  public String azione;
  public String dettagli;
  public LocalDateTime timestamp;

  public StoricoRichiamoDto()
  {
  }

  public StoricoRichiamoDto(
      Long id,
      Long richiamoId,
      Long operatoreId,
      String azione,
      String dettagli,
      LocalDateTime timestamp)
  {
    this.id = id;
    this.richiamoId = richiamoId;
    this.operatoreId = operatoreId;
    this.azione = azione;
    this.dettagli = dettagli;
    this.timestamp = timestamp;
  }
}
