package dev.crm.module.init.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OwnerAccountDto
{
  @NotBlank
  public String username;

  public String password;  // Only for creation

  public String nome;
  public String cognome;

  @NotBlank
  @Email
  public String email;

  public String telefono;
  public Integer twoFactorEnabled;

  public OwnerAccountDto()
  {
  }
}
