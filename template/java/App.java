package {{APP_PACKAGE}};

import {{APP_PACKAGE}}.handler.ChangePasswordHandler;
import {{APP_PACKAGE}}.handler.LoginHandler;
import {{APP_PACKAGE}}.handler.LogoutHandler;
import {{APP_PACKAGE}}.handler.RefreshHandler;
import {{APP_PACKAGE}}.handler.SessionHandler;
import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HandlerAdapter;
import dev.jms.util.Mail;
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

    Auth.init(
      config.get("jwt.secret", "dev-secret-change-in-production"),
      config.getInt("jwt.access.expiry.seconds", 900)
    );

    Mail.init(config);

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    );

    paths = new PathTemplateHandler(staticHandler)
      .add("/",     redirect("/home/main.html"))
      .add("/api/auth/login",   route(new LoginHandler(),   DB.getDataSource()))
      .add("/api/auth/session", route(new SessionHandler(), DB.getDataSource()))
      .add("/api/auth/logout",  route(new LogoutHandler(),  DB.getDataSource()))
      .add("/api/auth/refresh",         route(new RefreshHandler(),        DB.getDataSource()))
      .add("/api/auth/change-password", route(new ChangePasswordHandler(), DB.getDataSource()));

    // Aggiungere qui i propri handler:
    // .add("/api/users",      route(new UserHandler(), dataSource))
    // .add("/api/users/{id}", route(new UserHandler(), dataSource))

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static HttpHandler redirect(String location)
  {
    return exchange -> {
      exchange.setStatusCode(302);
      exchange.getResponseHeaders().put(Headers.LOCATION, location);
      exchange.endExchange();
    };
  }

  private static HandlerAdapter route(Handler handler)
  {
    return new HandlerAdapter(handler);
  }

  private static HandlerAdapter route(Handler handler, javax.sql.DataSource dataSource)
  {
    return new HandlerAdapter(handler, dataSource);
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
