package dev.jms.util;

/** Interfaccia per gli handler HTTP. Sovrascrivere solo i metodi necessari.
 *  I metodi non implementati rispondono automaticamente con 405 Method Not Allowed. */
public interface Handler
{
  default void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
  }

  default void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
  }

  default void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
  }

  default void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
  }
}
