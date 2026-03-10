package {{APP_PACKAGE}}.auth;

import {{APP_PACKAGE}}.auth.handler.ChangePasswordHandler;
import {{APP_PACKAGE}}.auth.handler.ForgotPasswordHandler;
import {{APP_PACKAGE}}.auth.handler.LoginHandler;
import {{APP_PACKAGE}}.auth.handler.LogoutHandler;
import {{APP_PACKAGE}}.auth.handler.RefreshHandler;
import {{APP_PACKAGE}}.auth.handler.ResetPasswordHandler;
import {{APP_PACKAGE}}.auth.handler.SessionHandler;
import {{APP_PACKAGE}}.auth.handler.TwoFactorHandler;
import dev.jms.util.Config;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;

import javax.sql.DataSource;

/** Registra le rotte del modulo auth (autenticazione). */
public class Routes
{
  /** Registra le rotte /api/auth/*. */
  public static void register(PathTemplateHandler paths, DataSource ds, Config config)
  {
    paths.add("/api/auth/login",           new HandlerAdapter(new LoginHandler(), ds));
    paths.add("/api/auth/logout",          new HandlerAdapter(new LogoutHandler(), ds));
    paths.add("/api/auth/refresh",         new HandlerAdapter(new RefreshHandler(), ds));
    paths.add("/api/auth/session",         new HandlerAdapter(new SessionHandler(), ds));
    paths.add("/api/auth/2fa",             new HandlerAdapter(new TwoFactorHandler(), ds));
    paths.add("/api/auth/forgot-password", new HandlerAdapter(new ForgotPasswordHandler(), ds));
    paths.add("/api/auth/reset-password",  new HandlerAdapter(new ResetPasswordHandler(), ds));
    paths.add("/api/auth/change-password", new HandlerAdapter(new ChangePasswordHandler(), ds));
  }
}
