package dev.jms.app.home.handler;

import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;

/**
 * Handler per il modulo home.
 * Restituisce un messaggio di benvenuto statico.
 */
public class HelloHandler
{
  /**
   * GET /api/home/hello — messaggio di benvenuto.
   */
  public void hello(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out("Hello, World!")
       .send();
  }
}
