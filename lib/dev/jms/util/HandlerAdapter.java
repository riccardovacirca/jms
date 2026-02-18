package dev.jms.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * Adattatore tra l'interfaccia Handler della libreria e HttpHandler di Undertow.
 * Esegue il dispatch su un thread bloccante prima di invocare l'handler,
 * rendendo disponibile la lettura del body tramite HttpRequest.getBody().
 * In caso di eccezione non gestita risponde con 500.
 */
public class HandlerAdapter implements HttpHandler
{

  private final Handler handler;

  public HandlerAdapter(Handler handler)
  {
    this.handler = handler;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception
  {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
      return;
    }

    exchange.startBlocking();

    try {
      handler.handle(new HttpRequest(exchange), new HttpResponse(exchange));
    } catch (Exception e) {
      if (!exchange.isResponseStarted()) {
        exchange.setStatusCode(500);
        exchange.getResponseHeaders().put(
          io.undertow.util.Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(
          Json.encode(Map.of("status", "error", "message",
            e.getMessage() != null ? e.getMessage() : "Errore interno")));
      }
    }
  }
}
