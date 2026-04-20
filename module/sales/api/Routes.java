package dev.jms.app.sales;

import dev.jms.app.sales.handler.CampagneHandler;
import dev.jms.app.sales.handler.ContattiHandler;
import dev.jms.app.sales.handler.ImporterHandler;
import dev.jms.app.sales.handler.ListeHandler;
import dev.jms.app.sales.handler.StatsHandler;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/** Registra le rotte HTTP del modulo sales. */
public class Routes
{
  /**
   * Aggiunge le rotte del modulo al router.
   *
   * @param router router dell'applicazione
   * @param config configurazione applicazione
   */
  public static void register(Router router, Config config)
  {
    ContattiHandler contatti;
    ListeHandler liste;
    ImporterHandler importer;
    CampagneHandler campagne;
    StatsHandler stats;

    contatti = new ContattiHandler();
    liste = new ListeHandler();
    importer = new ImporterHandler(config);
    campagne = new CampagneHandler();
    stats = new StatsHandler();

    router.route(HttpMethod.GET,    "/api/sales/stats",                     stats::stats);

    router.route(HttpMethod.GET,    "/api/sales/contatti",                  contatti::list);
    router.route(HttpMethod.POST,   "/api/sales/contatti",                  contatti::create);
    router.route(HttpMethod.GET,    "/api/sales/contatti/search",           contatti::search);
    router.route(HttpMethod.GET,    "/api/sales/contatti/{id}",             contatti::get);
    router.route(HttpMethod.PUT,    "/api/sales/contatti/{id}",             contatti::update);
    router.route(HttpMethod.DELETE, "/api/sales/contatti/{id}",             contatti::delete);
    router.route(HttpMethod.PUT,    "/api/sales/contatti/{id}/stato",       contatti::updateStato);
    router.route(HttpMethod.PUT,    "/api/sales/contatti/{id}/blacklist",   contatti::updateBlacklist);

    router.route(HttpMethod.GET,    "/api/sales/liste",                     liste::list);
    router.route(HttpMethod.POST,   "/api/sales/liste",                     liste::create);
    router.route(HttpMethod.GET,    "/api/sales/liste/default",             liste::getDefault);
    router.route(HttpMethod.GET,    "/api/sales/liste/{id}",                liste::get);
    router.route(HttpMethod.PUT,    "/api/sales/liste/{id}",                liste::update);
    router.route(HttpMethod.DELETE, "/api/sales/liste/{id}",                liste::delete);
    router.route(HttpMethod.PUT,    "/api/sales/liste/{id}/default",        liste::setDefault);
    router.route(HttpMethod.PUT,    "/api/sales/liste/{id}/stato",          liste::updateStato);
    router.route(HttpMethod.PUT,    "/api/sales/liste/{id}/scadenza",       liste::updateScadenza);
    router.route(HttpMethod.GET,    "/api/sales/liste/{id}/contatti",       liste::listContatti);
    router.route(HttpMethod.POST,   "/api/sales/liste/{id}/contatti",       liste::addContatto);
    router.route(HttpMethod.DELETE, "/api/sales/liste/{id}/contatti/{cid}", liste::removeContatto);

    router.route(HttpMethod.GET,    "/api/sales/campagne",                   campagne::list);
    router.route(HttpMethod.POST,   "/api/sales/campagne",                   campagne::create);
    router.route(HttpMethod.GET,    "/api/sales/campagne/{id}",              campagne::get);
    router.route(HttpMethod.PUT,    "/api/sales/campagne/{id}",              campagne::update);
    router.route(HttpMethod.DELETE, "/api/sales/campagne/{id}",              campagne::delete);
    router.route(HttpMethod.GET,    "/api/sales/campagne/{id}/liste",        campagne::listListe);
    router.route(HttpMethod.POST,   "/api/sales/campagne/{id}/liste",        campagne::addLista);
    router.route(HttpMethod.DELETE, "/api/sales/campagne/{id}/liste/{lid}",  campagne::removeLista);

    router.route(HttpMethod.GET,    "/api/sales/import/campi",              importer::campi);
    router.route(HttpMethod.POST,   "/api/sales/import/analyze",            importer::analyze);
    router.route(HttpMethod.PUT,    "/api/sales/import/{id}/mapping",       importer::mapping);
    router.route(HttpMethod.GET,    "/api/sales/import/{id}/validate",      importer::validate);
    router.route(HttpMethod.POST,   "/api/sales/import/{id}/execute",       importer::execute);
  }
}
