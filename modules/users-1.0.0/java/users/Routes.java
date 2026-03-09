package {{APP_PACKAGE}}.users;

import {{APP_PACKAGE}}.users.handler.GeneratePasswordHandler;
import {{APP_PACKAGE}}.users.handler.UserItemHandler;
import {{APP_PACKAGE}}.users.handler.UserListHandler;
import dev.jms.util.HandlerAdapter;
import dev.jms.util.Config;
import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;

/** Registra le rotte del modulo users. */
public class Routes
{
  /** Registra GET/POST /api/users e GET/PUT/DELETE /api/users/{id}. */
  public static void register(PathTemplateHandler paths, DataSource ds, Config config)
  {
    paths.add("/api/users/generate-password", new HandlerAdapter(new GeneratePasswordHandler(), ds));
    paths.add("/api/users",      new HandlerAdapter(new UserListHandler(config), ds));
    paths.add("/api/users/{id}", new HandlerAdapter(new UserItemHandler(), ds));
  }
}
