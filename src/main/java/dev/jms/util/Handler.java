package dev.jms.util;

/**
 * Interfaccia per gli handler HTTP.
 * Sovrascrivere solo i metodi necessari; i metodi non implementati
 * rispondono automaticamente con 405 Method Not Allowed.
 */
public interface Handler
{
  /** Gestisce GET; risponde 405 se non sovrascritto. */
  default void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405)
       .contentType("application/json")
       .err(true)
       .log("Method Not Allowed")
       .out(null)
       .send();
  }

  /** Gestisce POST; risponde 405 se non sovrascritto. */
  default void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405)
       .contentType("application/json")
       .err(true)
       .log("Method Not Allowed")
       .out(null)
       .send();
  }

  /** Gestisce PUT; risponde 405 se non sovrascritto. */
  default void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405)
       .contentType("application/json")
       .err(true)
       .log("Method Not Allowed")
       .out(null)
       .send();
  }

  /** Gestisce DELETE; risponde 405 se non sovrascritto. */
  default void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405)
       .contentType("application/json")
       .err(true)
       .log("Method Not Allowed")
       .out(null)
       .send();
  }
}
