package dev.jms.app;

import dev.jms.util.AsyncExecutor;
import dev.jms.util.Auth;
import dev.jms.util.JWTBlacklist;
import dev.jms.util.RateLimiter;
import dev.jms.util.Scheduler;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Mail;
import dev.jms.util.Router;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import org.flywaydb.core.Flyway;
import java.io.InputStream;
import java.util.Map;
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
    runMigrations();
    Scheduler.init(config, DB.getDataSource());
    checkModuleDependencies();

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

    // [MODULE_ROUTES]

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    // Shutdown hook per terminare gracefully le utility (AsyncExecutor, Scheduler, RateLimiter, JWTBlacklist)
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      AsyncExecutor.shutdown();
      Scheduler.shutdown();
      RateLimiter.shutdown();
      JWTBlacklist.shutdown();
    }));

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  /**
   * Verifica che le dipendenze dichiarate nei moduli installati siano tutte presenti.
   * Legge il manifest generato da cmd module install/remove.
   * Logga un warning per ogni dipendenza mancante, senza interrompere l'avvio.
   */
  @SuppressWarnings("unchecked")
  private static void checkModuleDependencies()
  {
    InputStream is;
    ObjectMapper mapper;
    Map<String, Map<String, Object>> installed;
    boolean ok;

    is = App.class.getClassLoader().getResourceAsStream("module/installed.json");
    if (is != null) {
      mapper = new ObjectMapper();
      installed = null;
      try {
        installed = mapper.readValue(is, new TypeReference<Map<String, Map<String, Object>>>() {});
      } catch (Exception e) {
        System.out.println("[warn] Impossibile leggere module/installed.json: " + e.getMessage());
      }
      if (installed != null) {
        ok = true;
        for (Map.Entry<String, Map<String, Object>> entry : installed.entrySet()) {
          Map<String, Object> info;
          Object depsObj;

          info = entry.getValue();
          depsObj = info.get("dependencies");
          if (depsObj instanceof Map) {
            Map<String, Object> deps;
            deps = (Map<String, Object>) depsObj;
            for (String dep : deps.keySet()) {
              boolean found;
              found = false;
              for (Map.Entry<String, Map<String, Object>> e2 : installed.entrySet()) {
                if (dep.equals(e2.getValue().get("name"))) {
                  found = true;
                }
              }
              if (!found) {
                System.out.println("[warn] Modulo '" + info.get("name") + "': dipendenza non installata: " + dep);
                ok = false;
              }
            }
          }
        }
        if (!ok) {
          System.out.println("[warn] Dipendenze moduli non soddisfatte — alcune funzionalita' potrebbero non essere disponibili");
        }
      }
    }
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
