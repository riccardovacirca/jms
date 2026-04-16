package dev.jms.app.crm;

import dev.jms.app.crm.handler.CampagneHandler;
import dev.jms.app.crm.handler.ContattiHandler;
import dev.jms.app.crm.handler.ImporterHandler;
import dev.jms.app.crm.handler.ListeHandler;
import dev.jms.app.crm.handler.TurnoHandler;
import dev.jms.util.Config;
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
   * @param config configurazione applicazione
   */
  public static void register(Router router, Config config)
  {
    ContattiHandler contatti;
    ListeHandler liste;
    ImporterHandler importer;
    TurnoHandler turni;

    CampagneHandler campagne;

    contatti = new ContattiHandler();
    liste = new ListeHandler();
    importer = new ImporterHandler(config);
    turni = new TurnoHandler();
    campagne = new CampagneHandler();

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
    router.route(HttpMethod.GET,    "/api/liste/default",             liste::getDefault);
    router.route(HttpMethod.GET,    "/api/liste/{id}",                liste::get);
    router.route(HttpMethod.PUT,    "/api/liste/{id}",                liste::update);
    router.route(HttpMethod.DELETE, "/api/liste/{id}",                liste::delete);
    router.route(HttpMethod.PUT,    "/api/liste/{id}/default",        liste::setDefault);
    router.route(HttpMethod.PUT,    "/api/liste/{id}/stato",          liste::updateStato);
    router.route(HttpMethod.PUT,    "/api/liste/{id}/scadenza",       liste::updateScadenza);
    router.route(HttpMethod.GET,    "/api/liste/{id}/contatti",       liste::listContatti);
    router.route(HttpMethod.POST,   "/api/liste/{id}/contatti",       liste::addContatto);
    router.route(HttpMethod.DELETE, "/api/liste/{id}/contatti/{cid}", liste::removeContatto);

    router.route(HttpMethod.GET,    "/api/crm/operatori/turni",              turni::list);
    router.route(HttpMethod.POST,   "/api/crm/operatori/turni",              turni::create);
    router.route(HttpMethod.PUT,    "/api/crm/operatori/turni/{id}",         turni::update);
    router.route(HttpMethod.DELETE, "/api/crm/operatori/turni/{id}",         turni::delete);
    router.route(HttpMethod.GET,    "/api/crm/operatori/turni/corrente",     turni::corrente);

    router.route(HttpMethod.GET,    "/api/campagne",                   campagne::list);
    router.route(HttpMethod.POST,   "/api/campagne",                   campagne::create);
    router.route(HttpMethod.GET,    "/api/campagne/{id}",              campagne::get);
    router.route(HttpMethod.PUT,    "/api/campagne/{id}",              campagne::update);
    router.route(HttpMethod.DELETE, "/api/campagne/{id}",              campagne::delete);
    router.route(HttpMethod.GET,    "/api/campagne/{id}/liste",        campagne::listListe);
    router.route(HttpMethod.POST,   "/api/campagne/{id}/liste",        campagne::addLista);
    router.route(HttpMethod.DELETE, "/api/campagne/{id}/liste/{lid}",  campagne::removeLista);

    router.route(HttpMethod.GET,    "/api/import/campi",              importer::campi);
    router.route(HttpMethod.POST,   "/api/import/analyze",            importer::analyze);
    router.route(HttpMethod.PUT,    "/api/import/{id}/mapping",       importer::mapping);
    router.route(HttpMethod.GET,    "/api/import/{id}/validate",      importer::validate);
    router.route(HttpMethod.POST,   "/api/import/{id}/execute",       importer::execute);
  }
}
