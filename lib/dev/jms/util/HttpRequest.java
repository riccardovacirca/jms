package dev.jms.util;

import io.undertow.server.handlers.Cookie;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.PathTemplateMatch;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
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

  /** Restituisce il primo valore del parametro querystring, null se assente. */
  public String getQueryParam(String name)
  {
    Deque<String> values;
    String result;

    values = exchange.getQueryParameters().get(name);
    result = (values != null && !values.isEmpty()) ? values.peekFirst() : null;
    return result;
  }

  /** Restituisce tutti i parametri querystring. */
  public Map<String, Deque<String>> getQueryParams()
  {
    return exchange.getQueryParameters();
  }

  /** Restituisce il primo valore dell'header, null se assente. */
  public String getHeader(String name)
  {
    HeaderValues values;
    String result;

    values = exchange.getRequestHeaders().get(name);
    result = values != null ? values.getFirst() : null;
    return result;
  }

  /** Restituisce il valore del cookie con il nome indicato, null se assente. */
  public String getCookie(String name)
  {
    Cookie cookie;
    String result;

    cookie = exchange.getRequestCookies().get(name);
    result = cookie != null ? cookie.getValue() : null;
    return result;
  }

  /** Restituisce i parametri estratti dal template URL (es. /api/users/{id} → {"id": "42"}).
   *  Richiede che la rotta sia registrata con PathTemplateHandler. */
  public Map<String, String> urlArgs()
  {
    PathTemplateMatch match;
    Map<String, String> result;

    match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    result = match != null ? match.getParameters() : new HashMap<>();
    return result;
  }

  /**
   * Legge e restituisce il body della request come stringa.
   * Richiede che l'exchange sia in modalità bloccante (garantito da HandlerAdapter).
   */
  public String getBody() throws Exception
  {
    String result;

    result = null;
    try (InputStream is = exchange.getInputStream()) {
      result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    return result;
  }
}
