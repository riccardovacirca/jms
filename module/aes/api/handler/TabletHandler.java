package dev.jms.app.module.aes.handler;

import dev.jms.app.module.aes.dto.TabletConfig;
import dev.jms.app.module.aes.helper.TabletConfigManager;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Handler per gestione configurazioni tablet Savino.
 * <p>
 * Fornisce endpoint per il recupero delle configurazioni tablet filtrate
 * per applicazione e dipartimento.
 * </p>
 */
public final class TabletHandler
{
  private final TabletConfigManager configManager;

  public TabletHandler()
  {
    this.configManager = new TabletConfigManager();
  }

  /**
   * GET /api/aes/tablets?app=...&department=...
   * <p>
   * Recupera le configurazioni tablet filtrate per app e department.
   * Entrambi i parametri sono opzionali. Se omessi, restituisce tutti i tablet.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void getTablets(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String app;
    String department;
    List<TabletConfig> tablets;
    List<Map<String, Object>> out;

    req.requireAuth();
    app = req.queryParam("app");
    department = req.queryParam("department");

    if (app != null || department != null) {
      tablets = configManager.getTabletsByApp(
        app != null ? app : "",
        department != null ? department : ""
      );
    } else {
      tablets = configManager.loadAllTablets();
    }

    out = tablets.stream()
      .map(t -> Map.of(
        "tabletName", (Object) t.tabletName,
        "tabletApp", t.tabletApp,
        "tabletDepartment", t.tabletDepartment,
        "tabletId", t.tabletId
      ))
      .toList();

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }
}
