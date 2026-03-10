package {{APP_PACKAGE}}.contatti;

import {{APP_PACKAGE}}.contatti.handler.ContattiHandler;
import {{APP_PACKAGE}}.contatti.handler.ContattiSearchHandler;
import {{APP_PACKAGE}}.contatti.handler.ContattoBlacklistHandler;
import {{APP_PACKAGE}}.contatti.handler.ContattoHandler;
import {{APP_PACKAGE}}.contatti.handler.ContattoStatoHandler;
import {{APP_PACKAGE}}.contatti.handler.ImportAnalyzeHandler;
import {{APP_PACKAGE}}.contatti.handler.ImportCampiHandler;
import {{APP_PACKAGE}}.contatti.handler.ImportExecuteHandler;
import {{APP_PACKAGE}}.contatti.handler.ImportMappingHandler;
import {{APP_PACKAGE}}.contatti.handler.ImportValidateHandler;
import {{APP_PACKAGE}}.contatti.handler.ListaContattiHandler;
import {{APP_PACKAGE}}.contatti.handler.ListaContattoHandler;
import {{APP_PACKAGE}}.contatti.handler.ListaHandler;
import {{APP_PACKAGE}}.contatti.handler.ListaScadenzaHandler;
import {{APP_PACKAGE}}.contatti.handler.ListaStatoHandler;
import {{APP_PACKAGE}}.contatti.handler.ListeHandler;
import dev.jms.util.HandlerAdapter;
import io.undertow.server.handlers.PathTemplateHandler;
import javax.sql.DataSource;

/**
 * Registra le rotte HTTP del modulo contatti.
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
    paths.add("/api/contatti",                    new HandlerAdapter(new ContattiHandler(), ds));
    paths.add("/api/contatti/search",             new HandlerAdapter(new ContattiSearchHandler(), ds));
    paths.add("/api/contatti/{id}",               new HandlerAdapter(new ContattoHandler(), ds));
    paths.add("/api/contatti/{id}/stato",         new HandlerAdapter(new ContattoStatoHandler(), ds));
    paths.add("/api/contatti/{id}/blacklist",     new HandlerAdapter(new ContattoBlacklistHandler(), ds));

    paths.add("/api/liste",                       new HandlerAdapter(new ListeHandler(), ds));
    paths.add("/api/liste/{id}",                  new HandlerAdapter(new ListaHandler(), ds));
    paths.add("/api/liste/{id}/stato",            new HandlerAdapter(new ListaStatoHandler(), ds));
    paths.add("/api/liste/{id}/scadenza",         new HandlerAdapter(new ListaScadenzaHandler(), ds));
    paths.add("/api/liste/{id}/contatti",         new HandlerAdapter(new ListaContattiHandler(), ds));
    paths.add("/api/liste/{id}/contatti/{cid}",   new HandlerAdapter(new ListaContattoHandler(), ds));

    paths.add("/api/import/campi",                new HandlerAdapter(new ImportCampiHandler(), ds));
    paths.add("/api/import/analyze",              new HandlerAdapter(new ImportAnalyzeHandler(), ds));
    paths.add("/api/import/{id}/mapping",         new HandlerAdapter(new ImportMappingHandler(), ds));
    paths.add("/api/import/{id}/validate",        new HandlerAdapter(new ImportValidateHandler(), ds));
    paths.add("/api/import/{id}/execute",         new HandlerAdapter(new ImportExecuteHandler(), ds));
  }
}
