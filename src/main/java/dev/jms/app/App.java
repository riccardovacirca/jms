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
 * Entry point dell'applicazione.
 * Inizializza i servizi (DB, Auth, Mail, AsyncExecutor),
 * esegue le migrazioni Flyway, registra le route e avvia il server Undertow.
 */
public class App
{
  /**
   * Avvia il server HTTP sulla porta configurata in application.properties.
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

    // === CARICAMENTO CONFIGURAZIONE ===
    config        = new Config();
    port          = config.getInt("server.port", 8080);
    asyncPoolSize = config.getInt("async.pool.size", 20);

    // === INIZIALIZZAZIONE UTILITY CORE ===

    // Crea HikariCP connection pool al database PostgreSQL
    // configurato in application.properties (db.host, db.port, db.name,
    // db.user, db.password)
    DB.init(config);

    // Configura JWT HS256 per access token con chiave segreta
    // e tempo di scadenza (jwt.secret, jwt.access.expiry.seconds).
    // Inizializza anche PBKDF2 per password hashing.
    Auth.init(config.get("jwt.secret", "dev-secret-change-in-production"),
              config.getInt("jwt.access.expiry.seconds", 900));

    // Configura client SMTP per invio email se abilitato (mail.enabled=true).
    // Legge host, port, auth, user, password, from da application.properties.
    Mail.init(config);

    // Crea thread pool dedicato per handler @Async con dimensione configurabile.
    // Thread pool separato da Undertow worker threads per operazioni
    // lente (async.pool.size).
    AsyncExecutor.init(asyncPoolSize);

    // === SETUP DATABASE E MODULI ===

    // Esegue migrazioni Flyway (db/migration/*.sql) PRIMA di Scheduler.init():
    // JobRunr crea le proprie tabelle al primo avvio e Flyway richiede uno schema
    // vuoto (o con history table) per procedere senza errori.
    runMigrations();

    // Inizializza JobRunr scheduler per job periodici.
    // Usa PostgreSQL come storage per job persistenti (tabelle jobrunr_*,
    // create automaticamente da JobRunr al primo avvio).
    // Configurabile via scheduler.enabled e scheduler.poll.interval.seconds.
    Scheduler.init(config, DB.getDataSource());

    // Verifica dipendenze dichiarate in module/installed.json.
    // Logga warning se moduli richiesti non sono installati. Non blocca l'avvio.
    checkModuleDependencies();

    // === CONFIGURAZIONE ROUTER E HANDLER ===

    // Ottiene il DataSource per passarlo al Router (usato da HandlerAdapter
    // per fornire istanza DB a ogni handler)
    ds = DB.getDataSource();

    // ResourceHandler per servire file statici dalla classpath
    // (frontend Vite build in src/main/resources/static/, incluso nel JAR).
    // setWelcomeFiles("index.html") serve index.html per richieste a directory.
    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    ).setWelcomeFiles("index.html");

    // PathTemplateHandler gestisce routing Undertow con fallback a staticHandler.
    // Se nessuna route API matcha, serve file statici (SPA routing lato client).
    paths  = new PathTemplateHandler(staticHandler);

    // Router wrapper per registrare route con HandlerAdapter automatico.
    // Fornisce metodi route() e async() per route con path parameters.
    router = new Router(paths, ds);

    // === REGISTRAZIONE ROUTE ===

    // Status endpoint pubblico — verifica che l'applicazione sia attiva.
    // Ritorna JSON standard {"err":false,"log":null,"out":"Application is running"}.
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
    // Marker per inserimento route da moduli installati.
    // cmd module import inserisce chiamate a Routes.register(router) qui.

    // === AVVIO SERVER ===

    // Crea server Undertow HTTP/1.1 sulla porta configurata (default: 8080).
    // Ascolta su tutte le interfacce (0.0.0.0), usa paths come root handler.
    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    // Shutdown hook per terminare gracefully le utility.
    // Chiude thread pool AsyncExecutor, ferma Scheduler, pulisce RateLimiter
    // e JWTBlacklist in-memory cache.
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
   * Verifica che le dipendenze dichiarate nei moduli installati siano presenti.
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
