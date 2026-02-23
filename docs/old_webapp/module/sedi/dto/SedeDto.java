package dev.crm.module.sedi.dto;

import java.time.LocalDateTime;

public class SedeDto
{
  public Long id;
  public String nome;
  public String indirizzo;
  public String citta;
  public String cap;
  public String telefono;
  public String email;
  public String note;
  public Integer attiva;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public SedeDto()
  {
  }

  public SedeDto(
      Long id,
      String nome,
      String indirizzo,
      String citta,
      String cap,
      String telefono,
      String email,
      String note,
      Integer attiva,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.indirizzo = indirizzo;
    this.citta = citta;
    this.cap = cap;
    this.telefono = telefono;
    this.email = email;
    this.note = note;
    this.attiva = attiva;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
