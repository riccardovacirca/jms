package dev.jms.app.module.aes;

import dev.jms.app.module.aes.handler.FileHandler;
import dev.jms.app.module.aes.handler.HtmlSignHandler;
import dev.jms.app.module.aes.handler.NamirialHandler;
import dev.jms.app.module.aes.handler.SavinoHandler;
import dev.jms.app.module.aes.handler.SignHandler;
import dev.jms.app.module.aes.handler.TabletHandler;
import dev.jms.app.module.aes.helper.CleanupJob;
import dev.jms.util.Config;
import dev.jms.util.HttpMethod;
import dev.jms.util.Router;
import dev.jms.util.Scheduler;

/**
 * Registrazione route per il modulo AES (Advanced Electronic Signature).
 * <p>
 * Fornisce endpoint per firma digitale su PDF, firma HTML, gestione file,
 * integrazione piattaforme Namirial e Savino (firma remota DM7/Conserva),
 * e gestione configurazioni tablet.
 * </p>
 */
public final class Routes
{
  private Routes()
  {
  }

  /**
   * Registra tutte le route del modulo AES e schedule i job periodici.
   *
   * @param router router globale
   * @param config configurazione applicazione
   */
  public static void register(Router router, Config config)
  {
    SignHandler sign;
    HtmlSignHandler htmlSign;
    FileHandler file;
    NamirialHandler namirial;
    SavinoHandler savino;
    TabletHandler tablet;

    sign = new SignHandler();
    htmlSign = new HtmlSignHandler();
    file = new FileHandler();
    namirial = new NamirialHandler(config);
    savino = new SavinoHandler();
    tablet = new TabletHandler();

    // Firma digitale PDF
    router.route(HttpMethod.POST, "/api/aes/firma", sign::post);

    // Firma HTML → PDF con placeholder firma
    router.route(HttpMethod.POST, "/api/aes/firma-html", htmlSign::post);

    // Gestione file temporanei
    router.route(HttpMethod.POST, "/api/aes/file/upload", file::upload);
    router.route(HttpMethod.GET, "/api/aes/file/download", file::download);
    router.route(HttpMethod.DELETE, "/api/aes/file/delete", file::delete);

    // Integrazione Namirial (firma remota DM7/Conserva)
    router.route(HttpMethod.POST, "/api/aes/namirial/require-signature", namirial::requireSignature);
    router.route(HttpMethod.GET, "/api/aes/namirial/document/{docId}", namirial::getDocument);
    router.route(HttpMethod.POST, "/api/aes/namirial/move-signed", namirial::moveSigned);
    router.route(HttpMethod.POST, "/api/aes/namirial/relate", namirial::relateFiles);
    router.route(HttpMethod.GET, "/api/aes/namirial/folders", namirial::listFolders);
    router.route(HttpMethod.GET, "/api/aes/namirial/documents", namirial::listDocuments);

    // Integrazione Savino (firma remota DM7/Conserva) - credenziali da DB
    router.route(HttpMethod.POST, "/api/aes/savino/require-signature", savino::requireSignature);
    router.route(HttpMethod.GET, "/api/aes/savino/document/{docId}", savino::getDocument);
    router.route(HttpMethod.POST, "/api/aes/savino/move-signed", savino::moveSigned);
    router.route(HttpMethod.POST, "/api/aes/savino/relate", savino::relateFiles);
    router.route(HttpMethod.GET, "/api/aes/savino/folders", savino::listFolders);
    router.route(HttpMethod.GET, "/api/aes/savino/documents", savino::listDocuments);

    // CRUD configurazioni tablet (tabella aes_tablet_config)
    router.route(HttpMethod.GET, "/api/aes/tablets", tablet::list);
    router.route(HttpMethod.GET, "/api/aes/tablets/{tabletId}", tablet::get);
    router.route(HttpMethod.POST, "/api/aes/tablets", tablet::create);
    router.route(HttpMethod.PUT, "/api/aes/tablets/{id}", tablet::update);
    router.route(HttpMethod.DELETE, "/api/aes/tablets/{id}", tablet::delete);

    // Job schedulato: cleanup file temporanei (ogni giorno alle 2:00)
    Scheduler.register("aes-cleanup", "0 2 * * *", CleanupJob::run);
  }
}
