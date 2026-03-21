package dev.jms.app;

import dev.jms.util.AsyncExecutor;
import dev.jms.util.Auth;
import dev.jms.util.AuthToken;
import dev.jms.util.AuthTokenStore;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Json;
import dev.jms.util.Mail;
import dev.jms.util.Router;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import java.net.InetSocketAddress;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;

/**
 * Entry point dell'applicazione. Inizializza i servizi (DB, Auth, Mail, AsyncExecutor),
 * esegue le migrazioni Flyway, registra le route e avvia il server Undertow.
 */
public class App
{
  /**
   * Avvia il server HTTP sulla porta configurata in application.properties (default: 8080).
   */
  public static void main(String[] args)
  {
    Config              config;
    int                 port;
    int                 asyncPoolSize;
    ResourceHandler     staticHandler;
    PathTemplateHandler paths;
    Router              router;
    Undertow            server;
    DataSource          ds;

    config        = new Config();
    port          = config.getInt("server.port", 8080);
    asyncPoolSize = config.getInt("async.pool.size", 20);

    DB.init(config);
    Auth.init(config.get("jwt.secret", "dev-secret-change-in-production"),
              config.getInt("jwt.access.expiry.seconds", 900));
    Mail.init(config);
    AsyncExecutor.init(asyncPoolSize);
    AuthTokenStore.init(config.get("authn.tokens.file", "/app/config/tokens.csv"));
    runMigrations();

    ds = DB.getDataSource();

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    ).setWelcomeFiles("index.html");

    paths  = new PathTemplateHandler(staticHandler);
    router = new Router(paths, ds);

    // Status endpoint (pubblico)
    paths.add("/api/status", new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception
      {
        exchange.getResponseHeaders()
          .put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender()
          .send("{\"err\":false,\"log\":null,\"out\":\"Application is running\"}");
      }
    });

    // Token generation endpoint (solo localhost, verificato dal guard)
    paths.add("/api/authn/token", new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception
      {
        if (exchange.isInIoThread()) {
          exchange.dispatch(this);
        } else {
          String method;

          exchange.startBlocking();
          method = exchange.getRequestMethod().toString();

          if ("POST".equals(method)) {
            authnCreate(exchange);
          } else if ("GET".equals(method)) {
            authnStatus(exchange);
          } else if ("PUT".equals(method)) {
            authnUpdate(exchange);
          } else if ("DELETE".equals(method)) {
            authnRemove(exchange);
          } else {
            sendError(exchange, 405, "Method Not Allowed");
          }
        }
      }
    });

    // [MODULE_ROUTES]

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(buildGuard(paths))
      .build();

    // Shutdown hook per terminare gracefully AsyncExecutor
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      AsyncExecutor.shutdown();
    }));

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static void authnCreate(HttpServerExchange exchange) throws Exception
  {
    String                        raw;
    java.util.Map<?, ?>           body;
    String                        ip;
    long                          expiresAt;
    boolean                       enabled;
    AuthToken                     info;
    java.util.Map<String, Object> out;
    String                        payload;

    raw       = new String(exchange.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    body      = raw.isBlank() ? new java.util.HashMap<>() : Json.decode(raw, java.util.HashMap.class);
    ip        = body.containsKey("ip") ? String.valueOf(body.get("ip")) : "";
    expiresAt = body.containsKey("expiresAt") && body.get("expiresAt") instanceof Number
                  ? ((Number) body.get("expiresAt")).longValue() : 0L;
    enabled   = !body.containsKey("enabled") || Boolean.TRUE.equals(body.get("enabled"));

    info      = AuthTokenStore.generate(ip, expiresAt, enabled);
    out       = tokenToMap(info);
    payload   = "{\"err\":false,\"log\":null,\"out\":" + Json.encode(out) + "}";
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(payload);
  }

  private static void authnStatus(HttpServerExchange exchange) throws Exception
  {
    String                        hash;
    AuthToken                     info;
    String                        payload;

    hash = exchange.getQueryParameters().containsKey("hash")
             ? exchange.getQueryParameters().get("hash").peekFirst() : null;
    info = hash != null ? AuthTokenStore.get(hash) : null;

    if (info == null) {
      payload = "{\"err\":true,\"log\":\"Token non trovato\",\"out\":null}";
    } else {
      payload = "{\"err\":false,\"log\":null,\"out\":" + Json.encode(tokenToMap(info)) + "}";
    }
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(payload);
  }

  private static void authnUpdate(HttpServerExchange exchange) throws Exception
  {
    String                        raw;
    java.util.Map<?, ?>           body;
    String                        hash;
    String                        ip;
    Long                          expiresAt;
    Boolean                       enabled;
    AuthToken                     info;
    String                        payload;

    raw       = new String(exchange.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    body      = raw.isBlank() ? new java.util.HashMap<>() : Json.decode(raw, java.util.HashMap.class);
    hash      = body.containsKey("hash") ? String.valueOf(body.get("hash")) : null;
    ip        = body.containsKey("ip") ? String.valueOf(body.get("ip")) : null;
    expiresAt = body.containsKey("expiresAt") && body.get("expiresAt") instanceof Number
                  ? ((Number) body.get("expiresAt")).longValue() : null;
    enabled   = body.containsKey("enabled") ? Boolean.TRUE.equals(body.get("enabled")) : null;

    if (hash == null || hash.isBlank()) {
      payload = "{\"err\":true,\"log\":\"Campo 'hash' obbligatorio\",\"out\":null}";
    } else {
      info = AuthTokenStore.update(hash, ip, expiresAt, enabled);
      if (info == null) {
        payload = "{\"err\":true,\"log\":\"Token non trovato\",\"out\":null}";
      } else {
        payload = "{\"err\":false,\"log\":null,\"out\":" + Json.encode(tokenToMap(info)) + "}";
      }
    }
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(payload);
  }

  private static void authnRemove(HttpServerExchange exchange) throws Exception
  {
    String  hash;
    boolean removed;
    String  payload;

    hash    = exchange.getQueryParameters().containsKey("hash")
                ? exchange.getQueryParameters().get("hash").peekFirst() : null;
    removed = hash != null && AuthTokenStore.remove(hash);
    payload = removed
                ? "{\"err\":false,\"log\":null,\"out\":\"Token rimosso\"}"
                : "{\"err\":true,\"log\":\"Token non trovato\",\"out\":null}";
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(payload);
  }

  private static java.util.Map<String, Object> tokenToMap(AuthToken info)
  {
    java.util.Map<String, Object> map;

    map = new java.util.HashMap<>();
    map.put("token",     info.token);
    map.put("ip",        info.ip);
    map.put("createdAt", info.createdAt);
    map.put("expiresAt", info.expiresAt);
    map.put("enabled",   info.enabled);
    return map;
  }

  /**
   * Costruisce il guard handler che protegge tutti gli endpoint {@code /api/}.
   *
   * <ul>
   *   <li>{@code /api/status} — pubblico, nessun controllo</li>
   *   <li>{@code /api/authn/token} — solo localhost (127.0.0.1 / ::1)</li>
   *   <li>tutti gli altri {@code /api/**} — richiedono {@code Authorization: Bearer <token>}</li>
   *   <li>percorsi non-API (frontend statico) — passano sempre</li>
   * </ul>
   *
   * @param paths handler Undertow a cui delegare le richieste autorizzate
   * @return {@link HttpHandler} con la logica di guardia applicata
   */
  private static HttpHandler buildGuard(PathTemplateHandler paths)
  {
    HttpHandler guard;

    guard = new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception
      {
        String            path;
        InetSocketAddress addr;
        String            address;
        String            authHeader;
        String            token;
        boolean           isApiPath;
        boolean           isPublicPath;
        boolean           isAuthnPath;
        boolean           isLocalhost;
        boolean           hasValidToken;

        path          = exchange.getRequestPath();
        addr          = exchange.getSourceAddress();
        address       = (addr != null && addr.getAddress() != null)
                          ? addr.getAddress().getHostAddress() : "";
        isApiPath     = path.startsWith("/api/");
        isPublicPath  = "/api/status".equals(path);
        isAuthnPath   = "/api/authn/token".equals(path);
        isLocalhost   = "127.0.0.1".equals(address) || "::1".equals(address)
                          || "0:0:0:0:0:0:0:1".equals(address);
        authHeader    = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        token         = extractBearer(authHeader);
        hasValidToken = AuthTokenStore.isValid(token, address);

        if (!isApiPath || isPublicPath) {
          paths.handleRequest(exchange);
        } else if (isAuthnPath) {
          if (isLocalhost) {
            paths.handleRequest(exchange);
          } else {
            sendError(exchange, 403, "Accesso non consentito");
          }
        } else {
          if (hasValidToken) {
            paths.handleRequest(exchange);
          } else {
            sendError(exchange, 401, "Token non valido o assente");
          }
        }
      }
    };
    return guard;
  }

  private static String extractBearer(String header)
  {
    String token;

    token = (header != null && header.startsWith("Bearer "))
              ? header.substring(7).trim() : null;
    return token;
  }

  private static void sendError(HttpServerExchange exchange, int status, String message)
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

  private static void runMigrations()
  {
    if (!DB.isConfigured()) {
      System.out.println("[info] Flyway: nessun DataSource, migrazione saltata");
    } else {
      try {
        Flyway flyway;
        int applied;
        flyway = Flyway.configure()
          .dataSource(DB.getDataSource())
          .locations("classpath:db/migration")
          .load();
        applied = flyway.migrate().migrationsExecuted;
        System.out.println("[info] Flyway: " + applied + " migrazione/i applicata/e");
      } catch (Exception e) {
        System.err.println("[warn] Flyway migration fallita: " + e.getMessage());
      }
    }
  }
}
