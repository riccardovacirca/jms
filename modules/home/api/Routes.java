package dev.jms.app.home;

import dev.jms.app.home.handler.HelloHandler;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

import javax.sql.DataSource;

/**
 * Registra le rotte HTTP del modulo home.
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
    HelloHandler h = new HelloHandler();
    router.route(HttpMethod.GET, "/api/home/hello", h::hello);
  }
}
