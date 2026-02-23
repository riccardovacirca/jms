package dev.crm.module.operatori.dto;

import java.time.LocalDateTime;

public class AttivitaOperatoreDto
{
  public Long id;
  public Long operatoreId;
  public String azione;
  public String descrizione;
  public LocalDateTime timestamp;

  public AttivitaOperatoreDto()
  {
  }

  public AttivitaOperatoreDto(
      Long id, Long operatoreId, String azione, String descrizione, LocalDateTime timestamp)
  {
    this.id = id;
    this.operatoreId = operatoreId;
    this.azione = azione;
    this.descrizione = descrizione;
    this.timestamp = timestamp;
  }
}
