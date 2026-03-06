package dev.crm.module.auth.dto;

import java.time.LocalDateTime;

public class RefreshTokenDto
{
  public Long id;
  public String token;
  public Long utenteId;
  public LocalDateTime expiresAt;
  public Boolean revoked;
}
