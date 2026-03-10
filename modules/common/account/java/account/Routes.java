package {{APP_PACKAGE}}.account;

import {{APP_PACKAGE}}.account.handler.GeneratePasswordHandler;
import {{APP_PACKAGE}}.account.handler.UserItemHandler;
import {{APP_PACKAGE}}.account.handler.UserListHandler;
import dev.jms.util.Config;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;

/** Registra le rotte del modulo account (gestione account utenti). */
public class Routes
{
  /** Registra le rotte /api/account/*. */
  public static void register(PathTemplateHandler paths, DataSource ds, Config config)
  {
    paths.add("/api/account/generate-password", new HandlerAdapter(new GeneratePasswordHandler(), ds));
    paths.add("/api/account",       new HandlerAdapter(new UserListHandler(config), ds));
    paths.add("/api/account/{id}",  new HandlerAdapter(new UserItemHandler(), ds));
  }
}
