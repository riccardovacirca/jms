package dev.jms.util;

import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Registra le rotte HTTP associando metodo, path e handler in un'unica riga.
 *
 * <p>Utilizzo:
 * <pre>
 *   Router router = new Router(paths, ds);
 *   AccountHandler h = new AccountHandler();
 *   router.route(HttpMethod.GET,    "/api/user/accounts",     h::list);
 *   router.route(HttpMethod.POST,   "/api/user/accounts",     h::register);
 *   router.route(HttpMethod.GET,    "/api/user/accounts/sid", h::sid);
 *   router.route(HttpMethod.PUT,    "/api/user/accounts/sid", h::update);
 *   router.route(HttpMethod.DELETE, "/api/user/accounts/sid", h::delete);
 * </pre>
 *
 * <p>Più metodi sullo stesso path condividono un unico {@link HandlerAdapter}.
 */
public class Router
{
  private final PathTemplateHandler paths;
  private final DataSource ds;
  private final Map<String, HandlerAdapter> adapters;

  /** Costruttore. */
  public Router(PathTemplateHandler paths, DataSource ds)
  {
    this.paths    = paths;
    this.ds       = ds;
    this.adapters = new HashMap<>();
  }

  /**
   * Registra una rotta blocking (default).
   * L'handler viene eseguito su un worker thread di Undertow.
   *
   * @param method  metodo HTTP
   * @param path    path template (es. /api/user/accounts/{id})
   * @param handler metodo da invocare
   */
  public void route(HttpMethod method, String path, RouteHandler handler)
  {
    HandlerAdapter adapter;

    adapter = adapters.computeIfAbsent(path, p -> {
      HandlerAdapter a = new HandlerAdapter(ds);
      paths.add(p, a);
      return a;
    });
    adapter.register(method, handler);
  }

  /**
   * Registra una rotta asincrona.
   * L'handler viene eseguito su {@link AsyncExecutor} invece del worker thread di Undertow.
   * Usare per operazioni lente: query pesanti, chiamate API esterne, elaborazioni CPU-intensive.
   *
   * @param method  metodo HTTP
   * @param path    path template (es. /api/report/export)
   * @param handler metodo da invocare
   */
  public void async(HttpMethod method, String path, RouteHandler handler)
  {
    HandlerAdapter adapter;

    adapter = adapters.computeIfAbsent(path, p -> {
      HandlerAdapter a = new HandlerAdapter(ds);
      paths.add(p, a);
      return a;
    });
    adapter.registerAsync(method, handler);
  }
}
