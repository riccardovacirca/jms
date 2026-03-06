package dev.crm.module.auth.dto;

public class UserDto
{
  public Long id;
  public String username;
  public String password;
  public String ruolo;
  public boolean attivo;

  public UserDto() {}

  public UserDto(Long id, String username, String password, String ruolo, boolean attivo)
  {
    this.id = id;
    this.username = username;
    this.password = password;
    this.ruolo = ruolo;
    this.attivo = attivo;
  }
}
