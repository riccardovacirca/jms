package dev.jms.app.module.cti.vonage;

import dev.jms.app.module.cti.vonage.handler.AuthHandler;
import dev.jms.app.module.cti.vonage.handler.CallHandler;
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

    router.route(HttpMethod.POST,   "/api/cti/vonage/auth",                 auth::post);
    router.route(HttpMethod.GET,    "/api/cti/vonage/chiamate",             calls::list);
    router.async(HttpMethod.POST,   "/api/cti/vonage/answer",               calls::answer);
    router.async(HttpMethod.PUT,    "/api/cti/vonage/call/{uuid}/hangup",   calls::hangup);
    router.async(HttpMethod.POST,   "/api/cti/vonage/sdk/auth",             calls::sdkToken);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/sdk/auth",             calls::releaseSession);
  }
}
