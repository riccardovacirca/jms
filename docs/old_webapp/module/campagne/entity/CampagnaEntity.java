package dev.crm.module.campagne.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampagnaEntity
{
  public Long id;
  public String nome;
  public String descrizione;
  public String tipo;
  public Integer stato;
  public LocalDate dataInizio;
  public LocalDate dataFine;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public CampagnaEntity()
  {
  }

  public CampagnaEntity(
      Long id,
      String nome,
      String descrizione,
      String tipo,
      Integer stato,
      LocalDate dataInizio,
      LocalDate dataFine,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.descrizione = descrizione;
    this.tipo = tipo;
    this.stato = stato;
    this.dataInizio = dataInizio;
    this.dataFine = dataFine;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
