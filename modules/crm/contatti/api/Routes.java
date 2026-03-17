package dev.jms.app.contatti;

import dev.jms.app.contatti.handler.ContattiHandler;
import dev.jms.app.contatti.handler.ContattiSearchHandler;
import dev.jms.app.contatti.handler.ContattoBlacklistHandler;
import dev.jms.app.contatti.handler.ContattoHandler;
import dev.jms.app.contatti.handler.ContattoStatoHandler;
import dev.jms.app.contatti.handler.ImportAnalyzeHandler;
import dev.jms.app.contatti.handler.ImportCampiHandler;
import dev.jms.app.contatti.handler.ImportExecuteHandler;
import dev.jms.app.contatti.handler.ImportMappingHandler;
import dev.jms.app.contatti.handler.ImportValidateHandler;
import dev.jms.app.contatti.handler.ListaContattiHandler;
import dev.jms.app.contatti.handler.ListaContattoHandler;
import dev.jms.app.contatti.handler.ListaHandler;
import dev.jms.app.contatti.handler.ListaScadenzaHandler;
import dev.jms.app.contatti.handler.ListaStatoHandler;
import dev.jms.app.contatti.handler.ListeHandler;
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
