package dev.jms.app.module.aes.dto;

import java.time.LocalDateTime;

/**
 * DTO per configurazione tablet firma remota (Savino/Namirial).
 * <p>
 * Rappresenta una riga della tabella {@code aes_tablet_config}.
 * Include credenziali di autenticazione dm7auth per il provider specificato.
 * </p>
 */
public class AesTabletConfig
{
  public final Long id;
  public final Long accountId;
  public final String tabletId;
  public final String tabletName;
  public final String tabletApp;
  public final String tabletDepartment;
  public final String provider;
  public final String endpoint;
  public final String username;
  public final String password;
  public final Boolean enabled;
  public final LocalDateTime createdAt;
  public final LocalDateTime updatedAt;

  public AesTabletConfig(
    Long id,
    Long accountId,
    String tabletId,
    String tabletName,
    String tabletApp,
    String tabletDepartment,
    String provider,
    String endpoint,
    String username,
    String password,
    Boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
  )
  {
    this.id = id;
    this.accountId = accountId;
    this.tabletId = tabletId;
    this.tabletName = tabletName;
    this.tabletApp = tabletApp;
    this.tabletDepartment = tabletDepartment;
    this.provider = provider;
    this.endpoint = endpoint;
    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
