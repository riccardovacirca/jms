package dev.jms.app.cti.helper;

import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.Log;
import java.util.ArrayList;

/**
 * Logica di autenticazione locale del modulo cti tramite API key.
 * Verifica la chiave contro la proprietà {@code cti.api.key} e,
 * se valida, emette un JWT di accesso HS256.
 */
public class AuthHelper
{
  private static final Log log = Log.get(AuthHelper.class);

  private final Config config;

  /**
   * @param config configurazione applicazione (usata per leggere {@code cti.api.key})
   */
  public AuthHelper(Config config)
  {
    this.config = config;
  }

  /**
   * Verifica l'API key e, se valida, crea un JWT di accesso CTI.
   *
   * @param apiKey chiave da verificare
   * @return token JWT se la chiave è valida, {@code null} altrimenti
   */
  public String createToken(String apiKey)
  {
    String expected;
    String token;

    expected = config.get("cti.api.key", "");
    token = null;
    if (apiKey != null && !apiKey.isBlank() && apiKey.equals(expected)) {
      token = Auth.get().createAccessToken(0, "cti-api", "cti", new ArrayList<>(), false);
    } else {
      log.warn("Tentativo di accesso CTI con API key non valida");
    }
    return token;
  }
}
