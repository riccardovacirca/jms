package dev.jms.app.module.cti.vonage;

import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.handler.CallHandler;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;
import dev.jms.util.Scheduler;

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
    CallHandler calls;

    calls = new CallHandler(config);

    // assegna operatore e genera JWT SDK
    router.async(HttpMethod.POST, "/api/cti/vonage/sdk/auth", calls::sdkToken);
    // rilascia operatore a fine sessione
    router.route(HttpMethod.DELETE, "/api/cti/vonage/sdk/auth", calls::releaseSession);
    // webhook Vonage: NCCO operatore + avvio chiamata cliente
    router.async(HttpMethod.POST, "/api/cti/vonage/answer", calls::answer);
    // hangup operatore e cliente
    router.async(HttpMethod.PUT, "/api/cti/vonage/call/{uuid}/hangup", calls::hangup);
    // webhook Vonage: eventi Voice e RTC
    router.route(HttpMethod.POST, "/api/cti/vonage/event", calls::event);

    // crea utente Vonage e registra operatore in DB
    router.async(HttpMethod.POST, "/api/cti/vonage/admin/operator", calls::createOperator);

    // cleanup sessioni operatori scadute (ogni minuto)
    Scheduler.register("cti-session-cleanup", "* * * * *", OperatorDAO::releaseExpired);
  }
}
