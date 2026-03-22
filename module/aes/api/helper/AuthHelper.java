package dev.jms.app.aes.helper;

import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.Log;
import java.util.ArrayList;

/**
 * Logica di autenticazione del modulo aes tramite API key.
 */
public class AuthHelper
{
  private static final Log log = Log.get(AuthHelper.class);

  private final Config config;

  /**
   * @param config configurazione applicazione (usata per leggere {@code aes.api.key})
   */
  public AuthHelper(Config config)
  {
    this.config = config;
  }

  /**
   * Verifica l'API key e, se valida, crea un JWT di accesso.
   *
   * @param apiKey chiave da verificare
   * @return token JWT se la chiave è valida, {@code null} altrimenti
   */
  public String createToken(String apiKey)
  {
    String expected;
    String token;

    expected = config.get("aes.api.key", "");
    token = null;
    if (apiKey != null && !apiKey.isBlank() && apiKey.equals(expected)) {
      token = Auth.get().createAccessToken(0, "aes-api", "aes", new ArrayList<>(), false);
    } else {
      log.warn("Tentativo di accesso con API key non valida");
    }
    return token;
  }
}
