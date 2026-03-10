package {{APP_PACKAGE}}.home;

import {{APP_PACKAGE}}.home.handler.HelloHandler;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;
import javax.sql.DataSource;

/**
 * Registra le rotte HTTP del modulo home.
 */
public class Routes
{
  /**
   * Aggiunge le rotte del modulo al PathTemplateHandler.
   *
   * @param paths handler delle rotte Undertow
   * @param ds    datasource per le connessioni al database
   */
  public static void register(PathTemplateHandler paths, DataSource ds)
  {
    paths.add("/api/home/hello", new HandlerAdapter(new HelloHandler(), ds));
  }
}
