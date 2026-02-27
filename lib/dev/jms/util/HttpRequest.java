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
  private final byte[] bodyBytes; // Null se blocking mode
  private String bodyString; // Cache

  /**
   * Costruttore per modalità blocking (esistente, invariato).
   */
  public HttpRequest(HttpServerExchange exchange)
  {
    this.exchange = exchange;
    this.bodyBytes = null;
  }

  /**
   * Costruttore per modalità async.
   * @param exchange Exchange Undertow
   * @param bodyBytes Body già letto in modo non-blocking
   */
  public HttpRequest(HttpServerExchange exchange, byte[] bodyBytes)
  {
    this.exchange = exchange;
    this.bodyBytes = bodyBytes;
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
   * Restituisce il body come stringa.
   * - Modalità blocking: legge da InputStream (BLOCKING)
   * - Modalità async: converte byte[] già letto (NON-BLOCKING)
   */
  public String getBody() throws Exception
  {
    if (bodyString != null) {
      return bodyString; // Cache
    }

    if (bodyBytes != null) {
      // Async mode: body già disponibile
      bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
      return bodyString;
    }

    // Blocking mode: legge da InputStream (comportamento attuale)
    try (InputStream is = exchange.getInputStream()) {
      byte[] bytes = is.readAllBytes();
      bodyString = new String(bytes, StandardCharsets.UTF_8);
      return bodyString;
    }
  }

  /**
   * Restituisce il body come byte[].
   * Utile per upload binari.
   */
  public byte[] getBodyBytes() throws Exception
  {
    if (bodyBytes != null) {
      return bodyBytes; // Async mode
    }

    // Blocking mode
    try (InputStream is = exchange.getInputStream()) {
      return is.readAllBytes();
    }
  }
}
