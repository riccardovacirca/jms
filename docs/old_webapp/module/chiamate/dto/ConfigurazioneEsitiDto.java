package dev.crm.module.chiamate.dto;

public class ConfigurazioneEsitiDto
{
  public String codice;
  public String descrizione;
  public String tipoEsito; // POSITIVO, NEGATIVO, RICHIAMO, GENERICO
  public boolean abilitaRichiamo;
  public Integer durataMinima; // durata minima chiamata in secondi
  public boolean attivo;
}
