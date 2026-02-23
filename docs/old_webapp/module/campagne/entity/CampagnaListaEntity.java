package dev.crm.module.campagne.entity;

import java.time.LocalDateTime;

public class CampagnaListaEntity
{
  public Long id;
  public Long campagnaId;
  public Long listaId;
  public LocalDateTime createdAt;

  public CampagnaListaEntity()
  {
  }

  public CampagnaListaEntity(Long id, Long campagnaId, Long listaId, LocalDateTime createdAt)
  {
    this.id = id;
    this.campagnaId = campagnaId;
    this.listaId = listaId;
    this.createdAt = createdAt;
  }
}
