package dev.jms.app.contatti;

import dev.jms.app.contatti.handler.ContattiHandler;
import dev.jms.app.contatti.handler.ImporterHandler;
import dev.jms.app.contatti.handler.ListeHandler;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;

/**
 * Registra le rotte HTTP del modulo contatti.
 */
public class Routes
{
  /**
   * Aggiunge le rotte del modulo al router.
   *
   * @param router router dell'applicazione
   */
  public static void register(Router router)
  {
    ContattiHandler contatti;
    ListeHandler liste;
    ImporterHandler importer;

    contatti = new ContattiHandler();
    liste = new ListeHandler();
    importer = new ImporterHandler();

    router.route(HttpMethod.GET,    "/api/contatti",                  contatti::list);
    router.route(HttpMethod.POST,   "/api/contatti",                  contatti::create);
    router.route(HttpMethod.GET,    "/api/contatti/search",           contatti::search);
    router.route(HttpMethod.GET,    "/api/contatti/{id}",             contatti::get);
    router.route(HttpMethod.PUT,    "/api/contatti/{id}",             contatti::update);
    router.route(HttpMethod.DELETE, "/api/contatti/{id}",             contatti::delete);
    router.route(HttpMethod.PUT,    "/api/contatti/{id}/stato",       contatti::updateStato);
    router.route(HttpMethod.PUT,    "/api/contatti/{id}/blacklist",   contatti::updateBlacklist);

    router.route(HttpMethod.GET,    "/api/liste",                     liste::list);
    router.route(HttpMethod.POST,   "/api/liste",                     liste::create);
    router.route(HttpMethod.GET,    "/api/liste/{id}",                liste::get);
    router.route(HttpMethod.PUT,    "/api/liste/{id}",                liste::update);
    router.route(HttpMethod.DELETE, "/api/liste/{id}",                liste::delete);
    router.route(HttpMethod.PUT,    "/api/liste/{id}/stato",          liste::updateStato);
    router.route(HttpMethod.PUT,    "/api/liste/{id}/scadenza",       liste::updateScadenza);
    router.route(HttpMethod.GET,    "/api/liste/{id}/contatti",       liste::listContatti);
    router.route(HttpMethod.POST,   "/api/liste/{id}/contatti",       liste::addContatto);
    router.route(HttpMethod.DELETE, "/api/liste/{id}/contatti/{cid}", liste::removeContatto);

    router.route(HttpMethod.GET,    "/api/import/campi",              importer::campi);
    router.route(HttpMethod.POST,   "/api/import/analyze",            importer::analyze);
    router.route(HttpMethod.PUT,    "/api/import/{id}/mapping",       importer::mapping);
    router.route(HttpMethod.GET,    "/api/import/{id}/validate",      importer::validate);
    router.route(HttpMethod.POST,   "/api/import/{id}/execute",       importer::execute);
  }
}
