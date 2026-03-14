package {{APP_PACKAGE}}.user;

import {{APP_PACKAGE}}.user.handler.GeneratePasswordHandler;
import {{APP_PACKAGE}}.user.handler.UserItemHandler;
import {{APP_PACKAGE}}.user.handler.UserListHandler;
import dev.jms.util.Config;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;

/** Registra le rotte del modulo account (gestione account utenti). */
public class Routes
{
  /** Registra le rotte /api/user/*. */
  public static void register(PathTemplateHandler paths, DataSource ds, Config config)
  {
    paths.add("/api/user/generate-password", new HandlerAdapter(new GeneratePasswordHandler(), ds));
    paths.add("/api/user",       new HandlerAdapter(new UserListHandler(config), ds));
    paths.add("/api/user/{id}",  new HandlerAdapter(new UserItemHandler(), ds));
  }
}
