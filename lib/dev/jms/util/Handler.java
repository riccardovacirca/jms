package dev.jms.util;

/** Interfaccia funzionale per gli handler HTTP. Riceve request e response già wrappate. */
@FunctionalInterface
public interface Handler
{
  void handle(HttpRequest req, HttpResponse res) throws Exception;
}
