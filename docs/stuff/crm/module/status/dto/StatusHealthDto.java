package dev.crm.module.status.dto;

public class StatusHealthDto
{
  private String status;
  private Integer campagne;
  private Integer liste;
  private Integer contatti;
  private Integer operatori;
  private Integer agenti;
  private Integer sedi;
  private Integer chiamate;

  public StatusHealthDto()
  {
  }

  public StatusHealthDto(
      String status,
      Integer campagne,
      Integer liste,
      Integer contatti,
      Integer operatori,
      Integer agenti,
      Integer sedi,
      Integer chiamate)
  {
    this.status = status;
    this.campagne = campagne;
    this.liste = liste;
    this.contatti = contatti;
    this.operatori = operatori;
    this.agenti = agenti;
    this.sedi = sedi;
    this.chiamate = chiamate;
  }

  public String getStatus()
  {
    return status;
  }

  public Integer getCampagne()
  {
    return campagne;
  }

  public Integer getListe()
  {
    return liste;
  }

  public Integer getContatti()
  {
    return contatti;
  }

  public Integer getOperatori()
  {
    return operatori;
  }

  public Integer getAgenti()
  {
    return agenti;
  }

  public Integer getSedi()
  {
    return sedi;
  }

  public Integer getChiamate()
  {
    return chiamate;
  }
}
