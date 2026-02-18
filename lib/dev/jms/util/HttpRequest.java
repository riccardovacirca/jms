package dev.jms.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;

public class HttpRequest
{

  private final HttpServerExchange exchange;

  public HttpRequest(HttpServerExchange exchange)
  {
    this.exchange = exchange;
  }

  public String getMethod()
  {
    return exchange.getRequestMethod().toString();
  }

  public String getPath()
  {
    return exchange.getRequestPath();
  }

  /** Restituisce il primo valore del parametro querystring, null se assente */
  public String getQueryParam(String name)
  {
    Deque<String> values = exchange.getQueryParameters().get(name);
    return (values != null && !values.isEmpty()) ? values.peekFirst() : null;
  }

  /** Restituisce tutti i parametri querystring */
  public Map<String, Deque<String>> getQueryParams()
  {
    return exchange.getQueryParameters();
  }

  /** Restituisce il primo valore dell'header, null se assente */
  public String getHeader(String name)
  {
    HeaderValues values = exchange.getRequestHeaders().get(name);
    return values != null ? values.getFirst() : null;
  }

  /**
   * Legge e restituisce il body della request come stringa.
   * Richiede che l'exchange sia in modalità bloccante (garantito da HandlerAdapter).
   */
  public String getBody() throws Exception
  {
    try (InputStream is = exchange.getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
