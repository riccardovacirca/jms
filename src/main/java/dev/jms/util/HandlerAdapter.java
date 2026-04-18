package dev.jms.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import javax.sql.DataSource;
import java.util.EnumMap;
import java.util.Map;

/**
 * Adattatore tra {@link RouteHandler} e {@link HttpHandler} di Undertow.
 *
 * <p>Creato e popolato da {@link Router}. Ogni istanza corrisponde a un path
 * e può gestire più metodi HTTP tramite {@link #register(HttpMethod, RouteHandler)}.
 *
 * <p>Compatibilità backward: il costruttore
 * {@link #HandlerAdapter(Handler, DataSource)} accetta ancora i vecchi handler
 * che implementano {@link Handler}, registrando automaticamente tutti i metodi.
 *
 * <p>Threading:
 * <ul>
 *   <li>BLOCKING (default): dispatch su worker thread Undertow tramite {@code exchange.dispatch()}</li>
 *   <li>ASYNC: dispatch su {@link AsyncExecutor} — per operazioni lente (query pesanti, API esterne)</li>
 * </ul>
 *
 * <p>Gestione eccezioni:
 * <ul>
 *   <li>{@link UnauthorizedException} → HTTP 401</li>
 *   <li>Qualsiasi altra eccezione non intercettata → HTTP 500 con log ERROR</li>
 * </ul>
 */
public class HandlerAdapter implements HttpHandler
{
  private static final Log log = Log.get(HandlerAdapter.class);

  private final Map<HttpMethod, RouteHandler> routes;
  private final Map<HttpMethod, Boolean> asyncFlags;
  private final DataSource dataSource;

  /**
   * Costruttore per {@link Router} — package-private.
   * I metodi vengono registrati successivamente con {@link #register}.
   */
  HandlerAdapter(DataSource dataSource)
  {
    this.dataSource = dataSource;
    this.routes = new EnumMap<>(HttpMethod.class);
    this.asyncFlags = new EnumMap<>(HttpMethod.class);
  }

  /**
   * Costruttore backward-compat per handler che implementano {@link Handler}.
   *
   * @deprecated usare {@link Router} con method reference.
   */
  @Deprecated
  public HandlerAdapter(Handler handler, DataSource dataSource)
  {
    this(dataSource);
    routes.put(HttpMethod.GET, (req, res, session, db) -> handler.get(req, res, db));
    routes.put(HttpMethod.POST, (req, res, session, db) -> handler.post(req, res, db));
    routes.put(HttpMethod.PUT, (req, res, session, db) -> handler.put(req, res, db));
    routes.put(HttpMethod.DELETE, (req, res, session, db) -> handler.delete(req, res, db));
  }

  /**
   * Costruttore backward-compat senza DataSource.
   *
   * @deprecated usare {@link Router} con method reference.
   */
  @Deprecated
  public HandlerAdapter(Handler handler)
  {
    this(handler, null);
  }

  /**
   * Registra un {@link RouteHandler} blocking per il metodo HTTP indicato.
   * L'handler verrà eseguito su un worker thread di Undertow.
   *
   * @param method  metodo HTTP
   * @param handler handler da registrare
   */
  void register(HttpMethod method, RouteHandler handler)
  {
    routes.put(method, handler);
  }

  /**
   * Registra un {@link RouteHandler} asincrono per il metodo HTTP indicato.
   * L'handler verrà eseguito su {@link AsyncExecutor} invece dei worker thread di Undertow.
   *
   * @param method  metodo HTTP
   * @param handler handler da registrare
   */
  void registerAsync(HttpMethod method, RouteHandler handler)
  {
    routes.put(method, handler);
    asyncFlags.put(method, Boolean.TRUE);
  }

  /**
   * Entry point Undertow. Rileva se siamo sull'IO thread e fa dispatch
   * al worker thread (blocking) o all'AsyncExecutor (async).
   */
  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception
  {
    HttpMethod method;
    boolean async;

    if (exchange.isInIoThread()) {
      method = parseMethod(exchange.getRequestMethod().toString());
      async = method != null && asyncFlags.getOrDefault(method, Boolean.FALSE);
      if (async) {
        exchange.dispatch(AsyncExecutor.getExecutor(), () -> executeBlocking(exchange));
      } else {
        exchange.dispatch(this);
      }
    } else {
      executeBlocking(exchange);
    }
  }

  /**
   * Esegue l'handler sul thread corrente (worker o AsyncExecutor).
   * Apre la connessione DB, risolve l'handler per il metodo HTTP,
   * gestisce UnauthorizedException (401) e qualsiasi altra eccezione (500).
   */
  private void executeBlocking(HttpServerExchange exchange)
  {
    DB db;
    HttpResponse res;
    HttpRequest req;
    Session session;
    HttpMethod method;
    RouteHandler handler;

    exchange.startBlocking();
    db = dataSource != null ? new DB(dataSource) : null;
    res = new HttpResponse(exchange);

    try {
      if (db != null) {
        db.open();
      }
      req = new HttpRequest(exchange);
      session = new Session(req);
      method = parseMethod(exchange.getRequestMethod().toString());
      handler = method != null ? routes.get(method) : null;

      if (handler == null) {
        res.status(405)
           .contentType("application/json")
           .err(true)
           .log("Method Not Allowed")
           .out(null)
           .send();
      } else {
        res.setPreSendHook(() -> session.flush(res));
        handler.handle(req, res, session, db);
      }

    } catch (UnauthorizedException e) {
      if (!exchange.isResponseStarted()) {
        res.status(401)
           .contentType("application/json")
           .err(true)
           .log(e.getMessage())
           .out(null)
           .send();
      }
    } catch (Exception e) {
      log.error("Errore di sistema in handler per {}", exchange.getRequestPath(), e);
      if (!exchange.isResponseStarted()) {
        res.status(500)
           .contentType("application/json")
           .err(true)
           .log("Errore interno del server")
           .out(null)
           .send();
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private static HttpMethod parseMethod(String method)
  {
    HttpMethod result;

    result = null;
    try {
      result = HttpMethod.valueOf(method);
    } catch (IllegalArgumentException e) {
      result = null;
    }
    return result;
  }

  private void sendErrorResponse(HttpServerExchange exchange, int status, String message)
  {
    String payload;

    exchange.setStatusCode(status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    payload = String.format(
      "{\"err\":true,\"log\":\"%s\",\"out\":null}",
      message.replace("\"", "\\\"")
    );
    exchange.getResponseSender().send(payload);
  }
}
