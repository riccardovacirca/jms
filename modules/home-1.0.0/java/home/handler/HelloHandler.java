package {{APP_PACKAGE}}.home.handler;

import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;

/**
 * Handler per il modulo home.
 * Restituisce un messaggio di benvenuto statico.
 */
public class HelloHandler implements Handler
{
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out("Hello, World!")
       .send();
  }
}
