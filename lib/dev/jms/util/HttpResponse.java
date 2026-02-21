package dev.jms.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.util.LinkedHashMap;

/**
 * Builder fluente per la response HTTP.
 *
 * Tutti i metodi tranne header() e cookie() devono essere chiamati esattamente una volta.
 * send() deve essere chiamato per ultimo e provoca l'invio effettivo.
 *
 * Il body ha sempre il formato: {"err": bool, "log": string|null, "out": object|null}
 */
public class HttpResponse
{
  private final HttpServerExchange exchange;

  private int _status;
  private String _contentType;
  private boolean _err;
  private String _log;
  private Object _out;
  private boolean _statusSet;
  private boolean _ctSet;
  private boolean _errSet;
  private boolean _logSet;
  private boolean _outSet;

  public HttpResponse(HttpServerExchange exchange)
  {
    this.exchange = exchange;
    this._status = -1;
  }

  public HttpResponse status(int code)
  {
    _status = code;
    _statusSet = true;
    return this;
  }

  /** Può essere chiamato zero o più volte. */
  public HttpResponse header(String name, String value)
  {
    exchange.getResponseHeaders().put(new HttpString(name), value);
    return this;
  }

  public HttpResponse contentType(String type)
  {
    _contentType = type;
    _ctSet = true;
    return this;
  }

  public HttpResponse err(boolean value)
  {
    _err = value;
    _errSet = true;
    return this;
  }

  public HttpResponse log(String message)
  {
    _log = message;
    _logSet = true;
    return this;
  }

  public HttpResponse out(Object payload)
  {
    _out = payload;
    _outSet = true;
    return this;
  }

  /** Può essere chiamato zero o più volte. */
  public HttpResponse cookie(String name, String value, int maxAge)
  {
    exchange.setResponseCookie(new CookieImpl(name, value)
      .setHttpOnly(true)
      .setPath("/")
      .setMaxAge(maxAge)
      .setSameSiteMode("Strict"));
    return this;
  }

  /** Valida che status(), contentType(), err(), log() e out() siano stati tutti chiamati, poi invia. */
  public void send()
  {
    LinkedHashMap<String, Object> body;

    if (!_statusSet) {
      throw new IllegalStateException("status() non chiamato");
    }
    if (!_ctSet) {
      throw new IllegalStateException("contentType() non chiamato");
    }
    if (!_errSet) {
      throw new IllegalStateException("err() non chiamato");
    }
    if (!_logSet) {
      throw new IllegalStateException("log() non chiamato");
    }
    if (!_outSet) {
      throw new IllegalStateException("out() non chiamato");
    }

    body = new LinkedHashMap<>();
    body.put("err", _err);
    body.put("log", _log);
    body.put("out", _out);

    exchange.setStatusCode(_status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, _contentType);
    exchange.getResponseSender().send(Json.encode(body));
  }
}
