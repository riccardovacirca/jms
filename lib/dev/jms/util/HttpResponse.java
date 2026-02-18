package dev.jms.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

public class HttpResponse
{

  private final HttpServerExchange exchange;

  public HttpResponse(HttpServerExchange exchange)
  {
    this.exchange = exchange;
  }

  /** Imposta lo status code HTTP. Restituisce this per il chaining. */
  public HttpResponse status(int code)
  {
    exchange.setStatusCode(code);
    return this;
  }

  /** Aggiunge un header alla response. Restituisce this per il chaining. */
  public HttpResponse header(String name, String value)
  {
    exchange.getResponseHeaders().put(new HttpString(name), value);
    return this;
  }

  /** Invia una risposta testuale con il body fornito. */
  public void send(String body)
  {
    exchange.getResponseSender().send(body);
  }

  /** Serializza obj come JSON, imposta Content-Type e invia la risposta. */
  public void sendJson(Object obj)
  {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(Json.encode(obj));
  }
}
