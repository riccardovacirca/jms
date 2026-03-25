package dev.jms.app.module.aes.helper;

import dev.jms.app.module.aes.dto.TabletConfig;
import dev.jms.util.Json;
import dev.jms.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Manager per caricamento e filtro configurazioni tablet Savino.
 * <p>
 * Legge i file JSON presenti in {@code /app/config/tablets/} e li deserializza
 * in oggetti {@link TabletConfig}. Supporta filtro per app e department.
 * </p>
 */
public final class TabletConfigManager
{
  private static final Log log = Log.get(TabletConfigManager.class);
  private static final String TABLETS_CONFIG_DIR = "/app/config/tablets";

  /**
   * Carica tutte le configurazioni tablet dalla directory {@code /app/config/tablets/}.
   *
   * @return lista configurazioni tablet (può essere vuota se directory non esiste o è vuota)
   * @throws Exception se errore di lettura o parsing JSON
   */
  public List<TabletConfig> loadAllTablets() throws Exception
  {
    Path dir;
    Stream<Path> files;
    List<TabletConfig> tablets;

    dir = Paths.get(TABLETS_CONFIG_DIR);

    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      log.warn("Tablets config directory not found: {}", TABLETS_CONFIG_DIR);
      return new ArrayList<>();
    }

    tablets = new ArrayList<>();

    files = Files.list(dir);
    files.filter(p -> p.toString().endsWith(".json"))
      .forEach(p -> {
        try {
          TabletConfig config;

          config = loadTabletFromFile(p);
          tablets.add(config);
        } catch (IOException e) {
          log.error("Failed to load tablet config from {}: {}", p, e.getMessage());
        }
      });
    files.close();

    log.debug("Loaded {} tablet configurations", tablets.size());
    return tablets;
  }

  /**
   * Carica le configurazioni tablet filtrate per app e department.
   * <p>
   * Il filtro è case-insensitive. Se un parametro è stringa vuota, quel campo
   * non viene filtrato. Se entrambi sono vuoti, restituisce tutti i tablet.
   * </p>
   *
   * @param app        nome applicazione (es. "sales", "hr"), o stringa vuota per non filtrare
   * @param department nome dipartimento (es. "commercial", "admin"), o stringa vuota per non filtrare
   * @return lista configurazioni tablet filtrata
   * @throws Exception se errore
   */
  public List<TabletConfig> getTabletsByApp(String app, String department) throws Exception
  {
    List<TabletConfig> all;
    List<TabletConfig> filtered;

    all = loadAllTablets();

    if (app.isBlank() && department.isBlank()) {
      return all;
    }

    filtered = new ArrayList<>();

    for (TabletConfig t : all) {
      boolean appMatches;
      boolean deptMatches;

      appMatches = app.isBlank() || t.tabletApp.equalsIgnoreCase(app);
      deptMatches = department.isBlank() || t.tabletDepartment.equalsIgnoreCase(department);

      if (appMatches && deptMatches) {
        filtered.add(t);
      }
    }

    log.debug("Filtered tablets by app={}, department={}: {} results", app, department, filtered.size());
    return filtered;
  }

  /**
   * Carica una configurazione tablet da file JSON.
   *
   * @param path percorso file JSON
   * @return configurazione tablet
   * @throws IOException se errore lettura o parsing
   */
  private TabletConfig loadTabletFromFile(Path path) throws IOException
  {
    String json;
    Map<String, Object> map;
    String tabletName;
    String tabletApp;
    String tabletDepartment;
    String tabletId;

    json = Files.readString(path);
    map = Json.decode(json, Map.class);

    tabletName = (String) map.get("tabletName");
    tabletApp = (String) map.get("tabletApp");
    tabletDepartment = (String) map.get("tabletDepartment");
    tabletId = (String) map.get("tabletId");

    return new TabletConfig(tabletName, tabletApp, tabletDepartment, tabletId);
  }
}
