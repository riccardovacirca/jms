package dev.crm.module.init.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AziendaDto
{
  public Long id;

  @NotBlank
  public String ragioneSociale;

  public String formaGiuridica;
  public String partitaIva;
  public String codiceFiscale;
  public String codiceSdi;

  @Email
  public String pec;

  public String numeroRea;
  public String capitaleSociale;

  // Sede legale
  public String sedeLegaleIndirizzo;
  public String sedeLegaleCap;
  public String sedeLegaleCitta;
  public String sedeLegaleProvincia;
  public String sedeLegaleNazione;

  // Contatti
  public String telefonoGenerale;

  @Email
  public String emailGenerale;

  public String sitoWeb;
  public String referenteCommerciale;
  public String referenteTecnico;

  // Fatturazione
  public String intestatarioFatturazione;
  public String indirizzoFatturazione;
  public String iban;
  public String modalitaPagamento;
  public String regimeIva;

  public AziendaDto()
  {
  }
}
