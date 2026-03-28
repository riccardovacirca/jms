package dev.jms.util;

import io.undertow.server.HttpServerExchange;
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
 *
 * Supporta sia modalità blocking che async: usa sempre ResponseSender che è già non-blocking.
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
  private Runnable preSendHook;

  /**
   * Configura i flag di sicurezza per i cookie a livello globale.
   * Delega a {@link Cookie#configure(boolean, String)}.
   *
   * @param secure   se true, aggiunge il flag Secure (richiede HTTPS)
   * @param sameSite Strict (massima protezione CSRF) | Lax (bilanciato) | None (richiede Secure=true)
   */
  public static void configureCookies(boolean secure, String sameSite)
  {
    Cookie.configure(secure, sameSite);
  }

  public HttpResponse(HttpServerExchange exchange)
  {
    this.exchange = exchange;
    this._status = -1;
  }

  /**
   * Imposta il codice di stato HTTP della risposta.
   *
   * @param code codice di stato HTTP (es. 200, 401, 500)
   * @return this per chaining
   */
  public HttpResponse status(int code)
  {
    _status = code;
    _statusSet = true;
    return this;
  }

  /**
   * Aggiunge un header HTTP alla risposta. Può essere chiamato zero o più volte.
   *
   * @param name  nome dell'header
   * @param value valore dell'header
   * @return this per chaining
   */
  public HttpResponse header(String name, String value)
  {
    exchange.getResponseHeaders().put(new HttpString(name), value);
    return this;
  }

  /**
   * Imposta il Content-Type della risposta (es. {@code "application/json"}).
   *
   * @param type MIME type della risposta
   * @return this per chaining
   */
  public HttpResponse contentType(String type)
  {
    _contentType = type;
    _ctSet = true;
    return this;
  }

  /**
   * Imposta il campo {@code err} dell'envelope JSON.
   *
   * @param value {@code true} se la risposta rappresenta un errore business
   * @return this per chaining
   */
  public HttpResponse err(boolean value)
  {
    _err = value;
    _errSet = true;
    return this;
  }

  /**
   * Imposta il campo {@code log} dell'envelope JSON (messaggio leggibile per il client).
   *
   * @param message messaggio da includere nella risposta, o {@code null}
   * @return this per chaining
   */
  public HttpResponse log(String message)
  {
    _log = message;
    _logSet = true;
    return this;
  }

  /**
   * Imposta il campo {@code out} dell'envelope JSON (payload dati della risposta).
   *
   * @param payload oggetto da serializzare come JSON, o {@code null}
   * @return this per chaining
   */
  public HttpResponse out(Object payload)
  {
    _out = payload;
    _outSet = true;
    return this;
  }

  /**
   * Aggiunge un cookie alla risposta. Può essere chiamato zero o più volte.
   *
   * @param name   nome del cookie
   * @param value  valore del cookie
   * @param maxAge durata in secondi
   * @return this per chaining
   */
  public HttpResponse cookie(String name, String value, int maxAge)
  {
    exchange.setResponseCookie(Cookie.build(name, value, maxAge));
    return this;
  }

  /**
   * Registra un hook eseguito appena prima dell'invio della risposta, all'interno di
   * {@link #send()}, {@link #raw(String)} e {@link #download(byte[], String, String)}.
   * Usato da {@link HandlerAdapter} per invocare {@link Session#flush(HttpResponse)}
   * prima che gli header vengano committati.
   *
   * @param hook callback da eseguire pre-send
   */
  void setPreSendHook(Runnable hook)
  {
    this.preSendHook = hook;
  }

  /**
   * Cancella un cookie impostando {@code maxAge=0} e valore vuoto. Può essere chiamato zero o più volte.
   *
   * @param name nome del cookie da cancellare
   * @return this per chaining
   */
  public HttpResponse clearCookie(String name)
  {
    exchange.setResponseCookie(Cookie.buildCleared(name));
    return this;
  }

  /**
   * Invia una risposta con body grezzo, bypassando l'envelope JSON standard.
   * Utile per webhook che richiedono un formato di risposta specifico (es. Vonage NCCO).
   * Non richiede la chiamata a err(), log(), out().
   * Richiede che status() e contentType() siano stati chiamati.
   */
  public void raw(String body)
  {
    if (!_statusSet) {
      throw new IllegalStateException("status() non chiamato");
    }
    if (!_ctSet) {
      throw new IllegalStateException("contentType() non chiamato");
    }
    if (preSendHook != null) {
      preSendHook.run();
    }
    exchange.setStatusCode(_status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, _contentType);
    exchange.getResponseSender().send(body);
  }

  /**
   * Invia un file binario come download (Content-Disposition: attachment).
   * Imposta automaticamente Content-Type e Content-Disposition header.
   * Non richiede la chiamata a contentType(), err(), log(), out().
   * Richiede che status() sia stato chiamato.
   *
   * @param data        contenuto binario del file
   * @param filename    nome del file per il download (es. "document.pdf")
   * @param contentType MIME type del file (es. "application/pdf", "application/octet-stream")
   */
  public void download(byte[] data, String filename, String contentType)
  {
    String disposition;

    if (!_statusSet) {
      throw new IllegalStateException("status() non chiamato");
    }
    if (data == null) {
      throw new IllegalArgumentException("data cannot be null");
    }
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("filename cannot be null or empty");
    }
    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("contentType cannot be null or empty");
    }

    if (preSendHook != null) {
      preSendHook.run();
    }

    disposition = "attachment; filename=\"" + filename + "\"";

    exchange.setStatusCode(_status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    exchange.getResponseHeaders().put(new HttpString("Content-Disposition"), disposition);
    exchange.getResponseSender().send(java.nio.ByteBuffer.wrap(data));
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

    if (preSendHook != null) {
      preSendHook.run();
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
