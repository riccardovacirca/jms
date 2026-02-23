package dev.crm.module.liste.dto;

import java.time.LocalDateTime;

public class ListaContattoDto
{
  public Long id;
  public Long listaId;
  public Long contattoId;
  public LocalDateTime createdAt;

  // Dati del contatto
  public String nomeContatto;
  public String telefono;

  public ListaContattoDto()
  {
  }

  public ListaContattoDto(Long id, Long listaId, Long contattoId, LocalDateTime createdAt)
  {
    this.id = id;
    this.listaId = listaId;
    this.contattoId = contattoId;
    this.createdAt = createdAt;
  }

  public ListaContattoDto(
      Long id,
      Long listaId,
      Long contattoId,
      LocalDateTime createdAt,
      String nomeContatto,
      String telefono)
  {
    this.id = id;
    this.listaId = listaId;
    this.contattoId = contattoId;
    this.createdAt = createdAt;
    this.nomeContatto = nomeContatto;
    this.telefono = telefono;
  }
}
