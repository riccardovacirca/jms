package dev.jms.app.module.cti.vonage;

import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.handler.CallHandler;
import dev.jms.app.module.cti.vonage.handler.OperatorHandler;
import dev.jms.app.module.cti.vonage.handler.PrefissoHandler;
import dev.jms.app.module.cti.vonage.handler.SessioneOperatoreHandler;
import dev.jms.app.module.cti.vonage.handler.QueueHandler;
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
    OperatorHandler operators;
    PrefissoHandler prefissi;
    SessioneOperatoreHandler turni;
    QueueHandler queue;

    calls    = new CallHandler(config);
    operators = new OperatorHandler(config);
    prefissi = new PrefissoHandler();
    turni    = new SessioneOperatoreHandler();
    queue    = new QueueHandler();

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
    // storico chiamate (paginato)
    router.route(HttpMethod.GET,  "/api/cti/vonage/call",  calls::list);

    // CRUD operatori (admin)
    router.route(HttpMethod.GET,    "/api/cti/vonage/admin/operator",                  operators::list);
    router.route(HttpMethod.GET,    "/api/cti/vonage/admin/operator/{id}",             operators::get);
    router.async(HttpMethod.POST,   "/api/cti/vonage/admin/operator",                  operators::create);
    router.route(HttpMethod.PUT,    "/api/cti/vonage/admin/operator/{id}",             operators::update);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/admin/operator/{id}",             operators::delete);
    // sync operatori locali da Vonage
    router.async(HttpMethod.POST,   "/api/cti/vonage/admin/operator/sync",             operators::sync);
    // assegnazione account utente a operatore
    router.route(HttpMethod.PUT,    "/api/cti/vonage/admin/operator/{id}/account",     operators::assignAccount);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/admin/operator/{id}/account",     operators::unassignAccount);
    // lista account utenti per dropdown assegnazione
    router.route(HttpMethod.GET,    "/api/cti/vonage/admin/accounts",                  operators::accounts);

    // prefissi telefonici internazionali
    router.route(HttpMethod.GET, "/api/cti/vonage/prefissi", prefissi::list);

    // lista sessioni tecniche (admin)
    router.route(HttpMethod.GET, "/api/cti/vonage/admin/sessioni",    turni::list);
    // sessione tecnica corrente dell'operatore autenticato
    router.route(HttpMethod.GET, "/api/cti/vonage/sessione/corrente", turni::corrente);

    // coda personale operatore (admin) + rimozione forzata orfani
    router.route(HttpMethod.GET,    "/api/cti/vonage/admin/operator/{id}/queue",   queue::adminQueueByOperator);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/admin/queue/contatto/{id}",   queue::adminRimuoviContatto);

    // Coda contatti — prefissi più specifici prima di quelli generici
    router.route(HttpMethod.GET,    "/api/cti/vonage/queue/contact",                   queue::getCurrentContact);
    router.route(HttpMethod.GET,    "/api/cti/vonage/queue/contatti",                  queue::listPersonal);
    router.route(HttpMethod.POST,   "/api/cti/vonage/queue/contatti",                  queue::aggiungiPersonale);
    router.route(HttpMethod.GET,    "/api/cti/vonage/queue/next",                      queue::getNext);
    router.route(HttpMethod.GET,    "/api/cti/vonage/queue/stats",                     queue::getStats);
    router.route(HttpMethod.PUT,    "/api/cti/vonage/queue/contatto/{id}/pianifica",   queue::pianifica);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/queue/contatto/{id}/rimetti",     queue::rimettiInCoda);
    router.route(HttpMethod.DELETE, "/api/cti/vonage/queue/contatto/{id}",             queue::rimuoviContatto);
    router.route(HttpMethod.POST,   "/api/cti/vonage/queue",                         queue::addToQueue);
    router.route(HttpMethod.POST,   "/api/cti/vonage/queue/bulk",                    queue::addBulkToQueue);

    // cleanup sessioni operatori scadute (ogni minuto)
    Scheduler.register("cti-session-cleanup", "* * * * *", OperatorDAO::releaseExpired);
  }
}
