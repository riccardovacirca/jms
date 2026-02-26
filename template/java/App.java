package {{APP_PACKAGE}};

import dev.jms.util.Config;
import dev.jms.util.DB;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import org.flywaydb.core.Flyway;

public class App
{
  public static void main(String[] args)
  {
    Config config;
    int port;
    ResourceHandler staticHandler;
    PathTemplateHandler paths;
    Undertow server;

    config = new Config();
    port = config.getInt("server.port", 8080);

    DB.init(config);
    runMigrations();

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    );

    paths = new PathTemplateHandler(staticHandler)
      .add("/",          redirect("/home/main.html"))
      .add("/api/hello", hello());

    // Aggiungere qui i propri handler:
    // .add("/api/users",      new HandlerAdapter(UserHandler.class, DB.getDataSource()))
    // .add("/api/users/{id}", new HandlerAdapter(UserHandler.class, DB.getDataSource()))

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static HttpHandler hello()
  {
    return exchange -> {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
      exchange.getResponseSender().send("{\"err\":false,\"log\":null,\"out\":\"Hello, World!\"}");
    };
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
