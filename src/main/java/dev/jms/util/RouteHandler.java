package dev.jms.util;

/**
 * Interfaccia funzionale per un metodo handler associato a una singola rotta.
 * Usata da {@link Router} e {@link HandlerAdapter}.
 */
@FunctionalInterface
public interface RouteHandler
{
  /** Gestisce la richiesta HTTP. */
  void handle(HttpRequest req, HttpResponse res, DB db) throws Exception;
}
