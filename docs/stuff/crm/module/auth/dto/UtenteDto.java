package dev.crm.module.auth.dto;

import java.time.LocalDateTime;

public class UtenteDto
{
  public Long id;
  public String username;
  public String passwordHash;
  public String email;
  public String ruolo;
  public Boolean attivo;
  public String nome;
  public String cognome;
  public String telefono;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}
