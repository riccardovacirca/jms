package dev.crm.module.importer.dto;

import java.time.LocalDateTime;

public class CampoDizionarioDto
{
  public Long id;
  public String nomeCampo;
  public String etichetta;
  public String descrizione;
  public String tipoDato;
  public Boolean obbligatorio;
  public Integer ordine;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public CampoDizionarioDto()
  {
  }

  public CampoDizionarioDto(
      Long id,
      String nomeCampo,
      String etichetta,
      String descrizione,
      String tipoDato,
      Boolean obbligatorio,
      Integer ordine,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nomeCampo = nomeCampo;
    this.etichetta = etichetta;
    this.descrizione = descrizione;
    this.tipoDato = tipoDato;
    this.obbligatorio = obbligatorio;
    this.ordine = ordine;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
