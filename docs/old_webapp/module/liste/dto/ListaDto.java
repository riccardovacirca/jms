package dev.crm.module.liste.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ListaDto
{
  public Long id;
  public String nome;
  public String descrizione;
  public Boolean consenso;
  public Integer stato;
  public LocalDate scadenza;
  public Long contattiCount;
  public Boolean attiva;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public LocalDateTime deletedAt;

  public ListaDto()
  {
  }

  public ListaDto(
      Long id,
      String nome,
      String descrizione,
      Boolean consenso,
      Integer stato,
      LocalDate scadenza,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.descrizione = descrizione;
    this.consenso = consenso;
    this.stato = stato;
    this.scadenza = scadenza;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = null;
  }

  public ListaDto(
      Long id,
      String nome,
      String descrizione,
      Boolean consenso,
      Integer stato,
      LocalDate scadenza,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt)
  {
    this.id = id;
    this.nome = nome;
    this.descrizione = descrizione;
    this.consenso = consenso;
    this.stato = stato;
    this.scadenza = scadenza;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }
}
