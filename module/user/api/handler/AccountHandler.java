package dev.jms.app.user.handler;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.helper.AccountSearchHelper;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per le operazioni CRUD sull'entità Account.
 *
 * <p>Rotte gestite (registrate in Routes.java):
 * <ul>
 *   <li>GET    /api/user/accounts           - lista paginata (admin)</li>
 *   <li>GET    /api/user/accounts/{id}      - account per id (admin)</li>
 *   <li>GET    /api/user/accounts/sid       - account in sessione</li>
 *   <li>POST   /api/user/accounts           - registrazione</li>
 *   <li>POST   /api/user/root               - creazione account root</li>
 *   <li>PUT    /api/user/accounts/sid       - aggiornamento self</li>
 *   <li>DELETE /api/user/accounts/sid       - cancellazione soft self</li>
 * </ul>
 */
public class AccountHandler
{
  private static final String ROOT_USERNAME = "root";
  /**
   * GET /api/user/accounts — lista paginata. Richiede ruolo admin.
   */
  public void list(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    if (!"admin".equals(claims.get("ruolo"))) {
      res.status(403).contentType("application/json")
         .err(true).log("Accesso non autorizzato").out(null).send();
      return;
    }
    String search       = req.queryParam("search");
    String pageParam    = req.queryParam("page");
    String pageSizeParam = req.queryParam("pageSize");
    int page     = (pageParam     != null && !pageParam.isBlank())     ? Integer.parseInt(pageParam)     : 1;
    int pageSize = (pageSizeParam != null && !pageSizeParam.isBlank()) ? Integer.parseInt(pageSizeParam) : 0;
    AccountSearchHelper helper = new AccountSearchHelper(db);
    Map<String, Object> result = helper.getEntries(search, page, pageSize);
    res.status(200).contentType("application/json")
       .err(false).log(null).out(result).send();
  }

  /**
   * GET /api/user/accounts/{id} — account per id. Richiede ruolo admin.
   */
  public void byId(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    if (!"admin".equals(claims.get("ruolo"))) {
      res.status(403).contentType("application/json")
         .err(true).log("Accesso non autorizzato").out(null).send();
      return;
    }
    long id = Long.parseLong(req.urlArgs().get("id"));
    AccountDAO dao = new AccountDAO(db);
    HashMap<String, Object> account = dao.findByIdManagement(id);
    if (account == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Account non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(account).send();
  }

  /**
   * GET /api/user/accounts/sid — account in sessione. Richiede JWT.
   */
  public void sid(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    long accountId = Long.parseLong(claims.get("sub").toString());
    AccountDAO dao = new AccountDAO(db);
    HashMap<String, Object> account = dao.findSelf(accountId);
    if (account == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Account non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(account).send();
  }

  /**
   * POST /api/user/accounts — registrazione. Non richiede autenticazione.
   */
  public void register(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body = req.body();
    String username = (String) body.get("username");
    String email    = (String) body.get("email");
    String password = (String) body.get("password");
    Validator.required(username, "username");
    Validator.required(email, "email");
    Validator.required(password, "password");
    AccountDAO dao = new AccountDAO(db);
    if (dao.existsByUsername(username, null)) {
      res.status(200).contentType("application/json")
         .err(true).log("Username già in uso").out(null).send();
      return;
    }
    if (dao.existsByEmail(email, null)) {
      res.status(200).contentType("application/json")
         .err(true).log("Email già in uso").out(null).send();
      return;
    }
    dao.create(username, email, Auth.hashPassword(password), "operatore");
    res.status(200).contentType("application/json")
       .err(false).log("Account creato").out(null).send();
  }

  /**
   * PUT /api/user/accounts/sid — aggiornamento self. Richiede JWT.
   */
  public void update(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    long accountId = Long.parseLong(claims.get("sub").toString());
    HashMap<String, Object> body = req.body();
    String username = (String) body.get("username");
    String email    = (String) body.get("email");
    Validator.required(username, "username");
    Validator.required(email, "email");
    AccountDAO dao = new AccountDAO(db);
    if (dao.existsByUsername(username, accountId)) {
      res.status(200).contentType("application/json")
         .err(true).log("Username già in uso").out(null).send();
      return;
    }
    if (dao.existsByEmail(email, accountId)) {
      res.status(200).contentType("application/json")
         .err(true).log("Email già in uso").out(null).send();
      return;
    }
    dao.updateSelf(accountId, username, email, null);
    res.status(200).contentType("application/json")
       .err(false).log("Account aggiornato").out(null).send();
  }

  /**
   * DELETE /api/user/accounts/sid — cancellazione soft self. Richiede JWT.
   */
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    long accountId = Long.parseLong(claims.get("sub").toString());
    new AccountDAO(db).softDelete(accountId);
    res.status(200).contentType("application/json")
       .err(false).log("Account eliminato").out(null).send();
  }

  /**
   * POST /api/user/root — creazione account root. Non richiede autenticazione.
   * <p>
   * Crea l'account root se configurato e non già esistente.
   * Legge root.password e root.email dalla configurazione.
   * Se entrambi sono valorizzati e non esiste già un account con username "root",
   * crea l'account con ruolo 'admin'.
   * </p>
   * <p>
   * L'account root ha username fisso "root" e ruolo admin con tutti i privilegi.
   * </p>
   */
  public void createRoot(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String password;
    String email;
    String sql;
    List<HashMap<String, Object>> existing;
    String passwordHash;
    int rows;

    body = req.body();
    password = (String) body.get("password");
    email = (String) body.get("email");

    Validator.required(password, "password");
    Validator.required(email, "email");

    // Verifica se esiste già l'account root
    sql = "SELECT id FROM accounts WHERE username = ?";
    existing = db.select(sql, ROOT_USERNAME);

    if (!existing.isEmpty()) {
      res.status(200).contentType("application/json")
         .err(true).log("Account root già esistente").out(null).send();
      return;
    }

    // Crea l'account root con ruolo admin
    passwordHash = Auth.hashPassword(password);

    sql = "INSERT INTO accounts (username, email, password_hash, ruolo, must_change_password) " +
          "VALUES (?, ?, ?, 'admin', false)";
    rows = db.query(sql, ROOT_USERNAME, email, passwordHash);

    if (rows > 0) {
      res.status(200).contentType("application/json")
         .err(false).log("Account root creato con successo").out(null).send();
    } else {
      res.status(200).contentType("application/json")
         .err(true).log("Errore nella creazione dell'account root").out(null).send();
    }
  }
}
