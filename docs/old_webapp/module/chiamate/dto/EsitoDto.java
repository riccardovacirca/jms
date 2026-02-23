package dev.crm.module.chiamate.dto;

public class EsitoDto
{
  public Long id;
  public Long chiamataId;
  public Long contattoId;
  public Long campagnaId;
  public String codiceEsito;
  public String tipoEsito; // POSITIVO, NEGATIVO, RICHIAMO, GENERICO
  public Integer durata; // durata chiamata in secondi
  public String note;
  public String createdAt;
}
