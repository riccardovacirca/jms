package dev.jms.app.schedulertest;

import dev.jms.app.schedulertest.handler.SchedulerTestHandler;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;
import dev.jms.util.Scheduler;

/**
 * Registra le rotte e i job schedulati del modulo schedulertest.
 */
public class Routes
{
  /**
   * Aggiunge le rotte HTTP e registra il job ricorrente.
   *
   * @param router router dell'applicazione
   */
  public static void register(Router router)
  {
    SchedulerTestHandler h;

    h = new SchedulerTestHandler();

    router.route(HttpMethod.GET, "/api/schedulertest/log",    h::log);
    router.route(HttpMethod.GET, "/api/schedulertest/status", h::status);

    Scheduler.register("schedulertest-tick", "* * * * *", SchedulerTestHandler::tick);
  }
}
