package dev.jms.app.asynctest;

import dev.jms.app.asynctest.handler.AsyncTestHandler;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/**
 * Registra le rotte del modulo asynctest.
 */
public class Routes
{
  /**
   * Aggiunge le rotte del modulo al router.
   *
   * @param router router dell'applicazione
   */
  public static void register(Router router)
  {
    AsyncTestHandler h = new AsyncTestHandler();

    // Endpoint ASYNC — eseguiti su AsyncExecutor (thread pool dedicato)
    router.async(HttpMethod.GET, "/api/asynctest/slow-query",    h::slowQuery);
    router.async(HttpMethod.GET, "/api/asynctest/slow-task",     h::slowTask);

    // Endpoint BLOCKING — eseguiti sui worker thread di Undertow (confronto)
    router.route(HttpMethod.GET, "/api/asynctest/blocking-query", h::blockingQuery);

    // Stato del pool AsyncExecutor
    router.route(HttpMethod.GET, "/api/asynctest/status",         h::status);
  }
}
