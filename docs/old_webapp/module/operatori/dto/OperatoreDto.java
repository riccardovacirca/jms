package dev.crm.module.operatori.dto;

import java.time.LocalDateTime;

public class OperatoreDto
{
  public Long id;
  public String nome;
  public String cognome;
  public String username;
  public String email;
  public String telefono;
  public String statoAttuale;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public OperatoreDto()
  {
  }

  public OperatoreDto(
      Long id,
      String nome,
      String cognome,
      String username,
      String email,
      String telefono,
      String statoAttuale,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.cognome = cognome;
    this.username = username;
    this.email = email;
    this.telefono = telefono;
    this.statoAttuale = statoAttuale;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
