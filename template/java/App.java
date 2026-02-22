package {{APP_PACKAGE}};

import {{APP_PACKAGE}}.handler.ChangePasswordHandler;
import {{APP_PACKAGE}}.handler.LoginHandler;
import {{APP_PACKAGE}}.handler.LogoutHandler;
import {{APP_PACKAGE}}.handler.RefreshHandler;
import {{APP_PACKAGE}}.handler.SessionHandler;
import dev.jms.util.Auth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import dev.jms.util.Handler;
import dev.jms.util.HandlerAdapter;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import org.flywaydb.core.Flyway;
import java.io.InputStream;

public class App
{
  private static HikariDataSource dataSource;

  public static void main(String[] args)
  {
    Config config;
    int port;
    ResourceHandler staticHandler;
    PathTemplateHandler paths;
    Undertow server;

    config = new Config();
    port = config.getInt("server.port", 8080);

    initDataSource(config);
    runMigrations();

    Auth.init(
      config.get("jwt.secret", "dev-secret-change-in-production"),
      config.getInt("jwt.access.expiry.seconds", 900)
    );

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    );

    paths = new PathTemplateHandler(staticHandler)
      .add("/",     redirect("/home"))
      .add("/home", page("home/index.html"))
      .add("/auth", page("auth/index.html"))
      .add("/api/auth/login",   route(new LoginHandler(),   dataSource))
      .add("/api/auth/session", route(new SessionHandler(), dataSource))
      .add("/api/auth/logout",  route(new LogoutHandler(),  dataSource))
      .add("/api/auth/refresh",         route(new RefreshHandler(),        dataSource))
      .add("/api/auth/change-password", route(new ChangePasswordHandler(), dataSource));

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

  private static HttpHandler page(String filename)
  {
    return exchange -> {
      InputStream in;
      byte[] bytes;
      in = App.class.getClassLoader().getResourceAsStream("static/" + filename);
      if (in == null) {
        exchange.setStatusCode(404);
        exchange.endExchange();
      } else {
        bytes = in.readAllBytes();
        in.close();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getResponseSender().send(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
      }
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

  private static HandlerAdapter route(Handler handler)
  {
    return new HandlerAdapter(handler);
  }

  private static HandlerAdapter route(Handler handler, DataSource dataSource)
  {
    return new HandlerAdapter(handler, dataSource);
  }

  private static void initDataSource(Config config)
  {
    String host;
    String dbPort;
    String name;
    String user;
    String password;
    int poolSize;

    host = config.get("db.host", "");
    dbPort = config.get("db.port", "5432");
    name = config.get("db.name", "");
    user = config.get("db.user", "");
    password = config.get("db.password", "");
    poolSize = config.getInt("db.pool.size", 10);

    if (host.isBlank() || name.isBlank() || user.isBlank()) {
      System.out.println("[info] Database non configurato, pool non inizializzato");
    } else {
      try {
        HikariConfig hc;
        hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:postgresql://" + host + ":" + dbPort + "/" + name);
        hc.setUsername(user);
        hc.setPassword(password);
        hc.setMaximumPoolSize(poolSize);
        hc.setInitializationFailTimeout(-1);
        dataSource = new HikariDataSource(hc);
        System.out.println("[info] Pool database inizializzato (" + host + ":" + dbPort + "/" + name + ")");
      } catch (Exception e) {
        System.err.println("[warn] Inizializzazione pool fallita: " + e.getMessage());
      }
    }
  }

  private static void runMigrations()
  {
    if (dataSource == null) {
      System.out.println("[info] Flyway: nessun DataSource, migrazione saltata");
    } else {
      try {
        Flyway flyway;
        int applied;
        flyway = Flyway.configure()
          .dataSource(dataSource)
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
