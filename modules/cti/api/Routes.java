package dev.jms.app.cti;

import dev.jms.app.cti.handler.AuthHandler;
import dev.jms.app.cti.handler.CallHandler;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/**
 * Registra le rotte HTTP del modulo cti.
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
    CallHandler calls;

    auth  = new AuthHandler(config);
    calls = new CallHandler(config);

    router.route(HttpMethod.POST, "/api/cti/auth",                     auth::post);
    router.route(HttpMethod.GET,  "/api/cti/chiamate",                 calls::list);
    router.async(HttpMethod.POST, "/api/cti/answer",                   calls::answer);
    router.async(HttpMethod.PUT,  "/api/cti/call/{uuid}/hangup",       calls::hangup);
    router.async(HttpMethod.GET,  "/api/cti/sdk/auth",                 calls::sdkToken);
  }
}
