package dev.crm.module.agenti.entity;

import java.time.LocalDateTime;

public class SlotDisponibileEntity
{
  public Long agenteId;
  public String nomeAgente;
  public LocalDateTime dataOra;
  public Integer durataMinuti;

  public SlotDisponibileEntity()
  {
  }

  public SlotDisponibileEntity(
      Long agenteId, String nomeAgente, LocalDateTime dataOra, Integer durataMinuti)
  {
    this.agenteId = agenteId;
    this.nomeAgente = nomeAgente;
    this.dataOra = dataOra;
    this.durataMinuti = durataMinuti;
  }
}
