package dev.crm.module.auth.dto;

public class SessionDto
{
  public String token;
  public Long userId;
  public boolean attiva;
  public Long createdAt;

  public SessionDto() {}

  public SessionDto(String token, Long userId, boolean attiva, Long createdAt)
  {
    this.token = token;
    this.userId = userId;
    this.attiva = attiva;
    this.createdAt = createdAt;
  }
}
