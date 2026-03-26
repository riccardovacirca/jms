package dev.jms.app.module.aes.handler;

import dev.jms.app.module.aes.dao.AesTabletConfigDao;
import dev.jms.app.module.aes.dto.AesTabletConfig;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Validator;
import dev.jms.util.ValidationException;
import java.util.List;
import java.util.Map;

/**
 * Handler per gestione CRUD configurazioni tablet Savino/Namirial.
 * <p>
 * Fornisce endpoint per creazione, lettura, aggiornamento ed eliminazione
 * delle configurazioni tablet memorizzate nella tabella {@code aes_tablet_config}.
 * </p>
 * <p>
 * Separato da {@link SavinoHandler} che gestisce le operazioni sui documenti.
 * </p>
 */
public final class TabletHandler
{
  /**
   * GET /api/aes/tablets
   * <p>
   * Recupera tutte le configurazioni tablet.
   * Supporta filtri opzionali via query params:
   * - {@code accountId}: filtra per account
   * - {@code provider}: filtra per provider ('savino' o 'namirial')
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void list(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String accountIdParam;
    String provider;
    AesTabletConfigDao dao;
    List<AesTabletConfig> tablets;
    List<Map<String, Object>> out;

    req.requireAuth();

    accountIdParam = req.queryParam("accountId");
    provider = req.queryParam("provider");

    dao = new AesTabletConfigDao(db);

    if (accountIdParam != null) {
      Long accountId;
      accountId = Long.parseLong(accountIdParam);
      tablets = dao.getByAccountId(accountId);
    } else if (provider != null) {
      tablets = dao.getByProvider(provider);
    } else {
      tablets = dao.getAll();
    }

    out = tablets.stream()
      .map(t -> Map.of(
        "id", (Object) t.id,
        "accountId", t.accountId,
        "tabletId", t.tabletId,
        "tabletName", t.tabletName,
        "tabletApp", t.tabletApp,
        "tabletDepartment", t.tabletDepartment,
        "provider", t.provider,
        "endpoint", t.endpoint,
        "enabled", t.enabled
        // password omessa per sicurezza
      ))
      .toList();

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/aes/tablets/{tabletId}
   * <p>
   * Recupera dettaglio configurazione tablet per ID tablet.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String tabletId;
    AesTabletConfigDao dao;
    AesTabletConfig tablet;
    Map<String, Object> out;

    req.requireAuth();
    tabletId = req.urlArgs().get("tabletId");

    Validator.required(tabletId, "tabletId");

    dao = new AesTabletConfigDao(db);
    tablet = dao.getByTabletId(tabletId);

    if (tablet == null) {
      throw new ValidationException("Tablet non trovato: " + tabletId);
    }

    out = Map.ofEntries(
      Map.entry("id", tablet.id),
      Map.entry("accountId", tablet.accountId),
      Map.entry("tabletId", tablet.tabletId),
      Map.entry("tabletName", tablet.tabletName),
      Map.entry("tabletApp", tablet.tabletApp),
      Map.entry("tabletDepartment", tablet.tabletDepartment),
      Map.entry("provider", tablet.provider),
      Map.entry("endpoint", tablet.endpoint),
      Map.entry("username", tablet.username),
      Map.entry("enabled", tablet.enabled),
      Map.entry("createdAt", tablet.createdAt.toString()),
      Map.entry("updatedAt", tablet.updatedAt.toString())
      // password omessa per sicurezza
    );

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * POST /api/aes/tablets
   * <p>
   * Crea una nuova configurazione tablet.
   * </p>
   * <p>
   * Body (JSON):
   * </p>
   * <pre>{@code
   * {
   *   "accountId": 123,
   *   "tabletId": "tablet-001",
   *   "tabletName": "Tablet Vendite",
   *   "tabletApp": "sales",
   *   "tabletDepartment": "commercial",
   *   "provider": "savino",
   *   "endpoint": "https://api.conserva.cloud/api/v1",
   *   "username": "tablet1",
   *   "password": "secret"
   * }
   * }</pre>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void create(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> body;
    Long accountId;
    String tabletId;
    String tabletName;
    String tabletApp;
    String tabletDepartment;
    String provider;
    String endpoint;
    String username;
    String password;
    AesTabletConfigDao dao;
    Long id;
    Map<String, Object> out;

    req.requireAuth();
    body = req.body();

    accountId = ((Number) body.get("accountId")).longValue();
    tabletId = (String) body.get("tabletId");
    tabletName = (String) body.get("tabletName");
    tabletApp = (String) body.get("tabletApp");
    tabletDepartment = (String) body.get("tabletDepartment");
    provider = (String) body.get("provider");
    endpoint = (String) body.get("endpoint");
    username = (String) body.get("username");
    password = (String) body.get("password");

    if (accountId == null) throw new ValidationException("accountId is required");
    Validator.required(tabletId, "tabletId");
    Validator.required(tabletName, "tabletName");
    Validator.required(tabletApp, "tabletApp");
    Validator.required(tabletDepartment, "tabletDepartment");
    Validator.required(provider, "provider");
    Validator.required(endpoint, "endpoint");
    Validator.required(username, "username");
    Validator.required(password, "password");

    if (!"savino".equals(provider) && !"namirial".equals(provider)) {
      throw new ValidationException("provider deve essere 'savino' o 'namirial'");
    }

    dao = new AesTabletConfigDao(db);
    id = dao.insert(
      accountId,
      tabletId,
      tabletName,
      tabletApp,
      tabletDepartment,
      provider,
      endpoint,
      username,
      password
    );

    out = Map.of("id", id, "tabletId", tabletId);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log("Configurazione tablet creata")
       .out(out)
       .send();
  }

  /**
   * PUT /api/aes/tablets/{id}
   * <p>
   * Aggiorna una configurazione tablet esistente.
   * </p>
   * <p>
   * Body (JSON):
   * </p>
   * <pre>{@code
   * {
   *   "tabletName": "Tablet Vendite Updated",
   *   "tabletApp": "sales",
   *   "tabletDepartment": "commercial",
   *   "endpoint": "https://api.conserva.cloud/api/v1",
   *   "username": "tablet1",
   *   "password": "newsecret"
   * }
   * }</pre>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void update(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String idParam;
    Long id;
    Map<String, Object> body;
    String tabletName;
    String tabletApp;
    String tabletDepartment;
    String endpoint;
    String username;
    String password;
    AesTabletConfigDao dao;
    int rows;

    req.requireAuth();
    idParam = req.urlArgs().get("id");
    id = Long.parseLong(idParam);

    body = req.body();

    tabletName = (String) body.get("tabletName");
    tabletApp = (String) body.get("tabletApp");
    tabletDepartment = (String) body.get("tabletDepartment");
    endpoint = (String) body.get("endpoint");
    username = (String) body.get("username");
    password = (String) body.get("password");

    Validator.required(tabletName, "tabletName");
    Validator.required(tabletApp, "tabletApp");
    Validator.required(tabletDepartment, "tabletDepartment");
    Validator.required(endpoint, "endpoint");
    Validator.required(username, "username");
    Validator.required(password, "password");

    dao = new AesTabletConfigDao(db);
    rows = dao.update(
      id,
      tabletName,
      tabletApp,
      tabletDepartment,
      endpoint,
      username,
      password
    );

    if (rows == 0) {
      throw new ValidationException("Tablet non trovato con ID: " + id);
    }

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log("Configurazione tablet aggiornata")
       .out(Map.of("updated", true))
       .send();
  }

  /**
   * DELETE /api/aes/tablets/{id}
   * <p>
   * Disabilita una configurazione tablet (soft delete).
   * Non elimina permanentemente il record per preservare l'audit trail.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String idParam;
    Long id;
    AesTabletConfigDao dao;
    int rows;

    req.requireAuth();
    idParam = req.urlArgs().get("id");
    id = Long.parseLong(idParam);

    dao = new AesTabletConfigDao(db);
    rows = dao.disable(id);

    if (rows == 0) {
      throw new ValidationException("Tablet non trovato con ID: " + id);
    }

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log("Configurazione tablet disabilitata")
       .out(Map.of("deleted", true))
       .send();
  }
}
