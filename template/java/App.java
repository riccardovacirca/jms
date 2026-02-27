package {{APP_PACKAGE}};

import dev.jms.util.AsyncExecutor;
import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HandlerAdapter;
import dev.jms.util.Handler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;

public class App
{
  public static void main(String[] args)
  {
    Config config;
    int port;
    int asyncPoolSize;
    ResourceHandler staticHandler;
    PathTemplateHandler paths;
    Undertow server;
    DataSource ds;

    config = new Config();
    port = config.getInt("server.port", 8080);
    asyncPoolSize = config.getInt("async.pool.size", 20);

    DB.init(config);
    Auth.init(config.get("jwt.secret", "dev-secret-change-in-production"), config.getInt("jwt.access.expiry.seconds", 900));
    AsyncExecutor.init(asyncPoolSize);
    runMigrations();

    ds = DB.getDataSource();

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    );

    paths = new PathTemplateHandler(staticHandler)
      // Root redirect - always present
      .add("/", redirect("/index.html"));

    // Aggiungere qui i propri handler:
    // .add("/api/users",      route(new UserHandler(), ds))
    // .add("/api/users/{id}", route(new UserHandler(), ds))

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    // Shutdown hook per terminare gracefully AsyncExecutor
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      AsyncExecutor.shutdown();
    }));

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static HttpHandler route(Handler handler, DataSource ds)
  {
    return new HandlerAdapter(handler, ds);
  }

  private static HttpHandler redirect(String location)
  {
    return exchange -> {
      exchange.setStatusCode(302);
      exchange.getResponseHeaders().put(Headers.LOCATION, location);
      exchange.endExchange();
    };
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
