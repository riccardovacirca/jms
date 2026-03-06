package dev.crm.module.agenti.entity;

import java.time.LocalDateTime;

public class AgenteEntity
{
  public Long id;
  public String nome;
  public String cognome;
  public String email;
  public String telefono;
  public String note;
  public Integer attivo;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public AgenteEntity()
  {
  }

  public AgenteEntity(
      Long id,
      String nome,
      String cognome,
      String email,
      String telefono,
      String note,
      Integer attivo,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.cognome = cognome;
    this.email = email;
    this.telefono = telefono;
    this.note = note;
    this.attivo = attivo;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
