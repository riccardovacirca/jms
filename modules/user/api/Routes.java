package {{APP_PACKAGE}}.user;

import {{APP_PACKAGE}}.user.handler.UserHandler;
import {{APP_PACKAGE}}.user.handler.UserSettingsHandler;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;

/** Registra le rotte del modulo user (profilo utente e impostazioni). */
public class Routes
{
  /** Registra le rotte /api/user/*. */
  public static void register(PathTemplateHandler paths, DataSource ds)
  {
    paths.add("/api/user/settings",       new HandlerAdapter(new UserSettingsHandler(), ds));
    paths.add("/api/user/settings/{key}", new HandlerAdapter(new UserSettingsHandler(), ds));
    paths.add("/api/user",                new HandlerAdapter(new UserHandler(), ds));
  }
}
