package dev.crm.module.init.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SedeDto
{
  public Long id;

  @NotBlank
  public String nome;

  public String indirizzo;
  public String cap;
  public String citta;
  public String provincia;
  public String nazione;
  public Integer numeroPostazioni;
  public String responsabileNome;
  public String telefono;

  @Email
  public String email;

  public Integer attiva;

  public SedeDto()
  {
  }
}
