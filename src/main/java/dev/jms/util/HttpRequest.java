package dev.jms.util;

import io.undertow.server.handlers.Cookie;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
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
  private FormData formData; // Cache per multipart

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

    cookie = exchange.getRequestCookie(name);
    result = cookie != null ? cookie.getValue() : null;
    return result;
  }

  /**
   * Restituisce l'indirizzo IP del client.
   * Controlla prima X-Forwarded-For (per proxy/load balancer), poi getSourceAddress().
   *
   * @return indirizzo IP del client
   */
  public String getClientIP()
  {
    String forwarded;
    String[] ips;

    forwarded = getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      ips = forwarded.split(",");
      return ips[0].trim();
    }

    return exchange.getSourceAddress().getAddress().getHostAddress();
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

  /**
   * Restituisce i byte del file caricato via multipart/form-data per il campo indicato.
   * Restituisce null se il campo non esiste o non è un file.
   */
  public byte[] getMultipartFileBytes(String fieldName) throws Exception
  {
    FormData data;
    FormData.FormValue fileValue;
    byte[] result;

    data = parseMultipart();
    result = null;
    if (data != null) {
      fileValue = data.getFirst(fieldName);
      if (fileValue != null && fileValue.isFileItem()) {
        try (InputStream is = fileValue.getFileItem().getInputStream()) {
          result = is.readAllBytes();
        }
      }
    }
    return result;
  }

  /**
   * Restituisce il nome originale del file caricato via multipart/form-data per il campo indicato.
   * Restituisce null se il campo non esiste o non è un file.
   */
  public String getMultipartFilename(String fieldName) throws Exception
  {
    FormData data;
    FormData.FormValue fileValue;
    String result;

    data = parseMultipart();
    result = null;
    if (data != null) {
      fileValue = data.getFirst(fieldName);
      if (fileValue != null && fileValue.isFileItem()) {
        result = fileValue.getFileName();
      }
    }
    return result;
  }

  /** Parsa il body multipart/form-data e mette in cache il risultato. */
  private FormData parseMultipart() throws Exception
  {
    FormParserFactory factory;
    FormDataParser parser;

    if (formData == null) {
      factory = FormParserFactory.builder().build();
      parser = factory.createParser(exchange);
      if (parser != null) {
        formData = parser.parseBlocking();
      }
    }
    return formData;
  }

  /**
   * Parsa il body JSON e lo restituisce come mappa.
   * Restituisce una mappa vuota se il body è assente o vuoto.
   *
   * @return mappa chiave/valore del body JSON
   */
  @SuppressWarnings("unchecked")
  public java.util.HashMap<String, Object> body() throws Exception
  {
    String raw;

    raw = getBody();
    if (raw == null || raw.isBlank()) {
      return new java.util.HashMap<>();
    }
    return Json.decode(raw, java.util.HashMap.class);
  }

  /**
   * Alias di {@link #getQueryParam(String)}.
   *
   * @param name nome del parametro querystring
   * @return valore o null
   */
  public String queryParam(String name)
  {
    return getQueryParam(name);
  }

  /**
   * Alias di {@link #getCookie(String)}.
   *
   * @param name nome del cookie
   * @return valore o null
   */
  public String cookie(String name)
  {
    return getCookie(name);
  }

}