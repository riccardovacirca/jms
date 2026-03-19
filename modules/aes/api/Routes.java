package dev.jms.app.aes;

import dev.jms.app.aes.handler.AuthHandler;
import dev.jms.app.aes.handler.SignHandler;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/**
 * Registra le rotte HTTP del modulo aes.
 */
public class Routes
{
  /**
   * Aggiunge le rotte del modulo al router.
   *
   * @param router router dell'applicazione
   * @param config configurazione applicazione
   */
  public static void register(Router router, Config config)
  {
    AuthHandler auth;
    SignHandler sign;

    auth = new AuthHandler(config);
    sign = new SignHandler();
    router.route(HttpMethod.POST, "/api/aes/token", auth::post);
    router.route(HttpMethod.POST, "/api/aes/firma", sign::post);
  }
}
