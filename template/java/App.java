package {{APP_PACKAGE}};

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
    ResourceHandler staticHandler;
    PathTemplateHandler paths;
    Undertow server;
    DataSource ds;

    config = new Config();
    port = config.getInt("server.port", 8080);

    DB.init(config);
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
