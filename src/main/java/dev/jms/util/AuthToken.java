package dev.jms.util;

/**
 * Dati immutabili di un token di autorizzazione API.
 *
 * <p>Istanze create esclusivamente da {@link AuthTokenStore}.
 */
public class AuthToken
{
  /** Valore opaco del token (64 caratteri hex). */
  public final String  token;
  /** IP del customer associato (può essere stringa vuota). */
  public final String  ip;
  /** Timestamp di creazione in epoch millis. */
  public final long    createdAt;
  /** Timestamp di scadenza in epoch millis; 0 = nessuna scadenza. */
  public final long    expiresAt;
  /** Se il token è attivo. */
  public final boolean enabled;

  /**
   * Costruttore.
   *
   * @param token     valore opaco del token (64 caratteri hex)
   * @param ip        IP del customer associato (può essere stringa vuota)
   * @param createdAt timestamp di creazione in epoch millis
   * @param expiresAt timestamp di scadenza in epoch millis; 0 = nessuna scadenza
   * @param enabled   se il token è attivo
   */
  public AuthToken(String token, String ip, long createdAt, long expiresAt, boolean enabled)
  {
    this.token     = token;
    this.ip        = ip;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.enabled   = enabled;
  }
}
