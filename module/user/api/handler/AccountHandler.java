package dev.jms.app.user.handler;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.helper.AccountSearchHelper;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per le operazioni CRUD sull'entità Account.
 *
 * <p>Rotte gestite (registrate in Routes.java):
 * <ul>
 *   <li>GET    /api/user/accounts           - lista paginata (admin+)</li>
 *   <li>GET    /api/user/accounts/{id}      - account per id (admin+)</li>
 *   <li>GET    /api/user/accounts/sid       - account in sessione (user+)</li>
 *   <li>POST   /api/user/accounts           - crea account (admin+ per user, root per admin)</li>
 *   <li>POST   /api/user/root               - creazione account root una tantum (nessuna auth)</li>
 *   <li>PUT    /api/user/accounts/sid       - aggiornamento self (user+)</li>
 *   <li>DELETE /api/user/accounts/sid       - cancellazione soft self (user+)</li>
 * </ul>
 */
public class AccountHandler
{
  private static final String ROOT_USERNAME = "root";

  /**
   * GET /api/user/accounts — lista paginata. Richiede admin+.
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String search;
    String pageParam;
    String pageSizeParam;
    int page;
    int pageSize;
    AccountSearchHelper helper;
    Map<String, Object> result;

    session.require(Role.ADMIN, Permission.READ);
    search        = req.queryParam("search");
    pageParam     = req.queryParam("page");
    pageSizeParam = req.queryParam("pageSize");
    page     = (pageParam     != null && !pageParam.isBlank())     ? Integer.parseInt(pageParam)     : 1;
    pageSize = (pageSizeParam != null && !pageSizeParam.isBlank()) ? Integer.parseInt(pageSizeParam) : 0;
    helper   = new AccountSearchHelper(db);
    result   = helper.getEntries(search, page, pageSize);
    res.status(200).contentType("application/json")
       .err(false).log(null).out(result).send();
  }

  /**
   * GET /api/user/accounts/{id} — account per id. Richiede admin+.
   */
  public void byId(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    AccountDAO dao;
    HashMap<String, Object> account;

    session.require(Role.ADMIN, Permission.READ);
    id      = Long.parseLong(req.urlArgs().get("id"));
    dao     = new AccountDAO(db);
    account = dao.findByIdManagement(id);
    if (account == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Account non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(account).send();
  }

  /**
   * GET /api/user/accounts/sid — account in sessione. Richiede user+.
   */
  public void sid(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    AccountDAO dao;
    HashMap<String, Object> account;

    session.require(Role.USER, Permission.READ);
    dao     = new AccountDAO(db);
    account = dao.findSelf(session.sub());
    if (account == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Account non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(account).send();
  }

  /**
   * POST /api/user/accounts — crea un account.
   * <p>Autorizzazione in base al ruolo da creare:
   * <ul>
   *   <li>ruolo {@code user}  — richiede admin+</li>
   *   <li>ruolo {@code admin} — richiede root</li>
   * </ul>
   * La creazione di root è riservata all'endpoint {@code POST /api/user/root}.
   */
  public void register(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String email;
    String password;
    String ruolo;
    AccountDAO dao;

    body     = req.body();
    username = (String) body.get("username");
    email    = (String) body.get("email");
    password = (String) body.get("password");
    ruolo    = (String) body.get("ruolo");

    Validator.required(username, "username");
    Validator.required(email,    "email");
    Validator.required(password, "password");
    Validator.required(ruolo,    "ruolo");

    if ("user".equals(ruolo)) {
      session.require(Role.ADMIN, Permission.WRITE);
    } else if ("admin".equals(ruolo)) {
      session.require(Role.ROOT, Permission.WRITE);
    } else {
      res.status(200).contentType("application/json")
         .err(true).log("Ruolo non valido. Valori accettati: user, admin").out(null).send();
      return;
    }

    dao = new AccountDAO(db);
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
    dao.create(username, email, Auth.hashPassword(password), ruolo);
    res.status(200).contentType("application/json")
       .err(false).log("Account creato").out(null).send();
  }

  /**
   * PUT /api/user/accounts/sid — aggiornamento self. Richiede user+.
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String email;
    AccountDAO dao;

    session.require(Role.USER, Permission.WRITE);
    body     = req.body();
    username = (String) body.get("username");
    email    = (String) body.get("email");
    Validator.required(username, "username");
    Validator.required(email,    "email");
    dao = new AccountDAO(db);
    if (dao.existsByUsername(username, session.sub())) {
      res.status(200).contentType("application/json")
         .err(true).log("Username già in uso").out(null).send();
      return;
    }
    if (dao.existsByEmail(email, session.sub())) {
      res.status(200).contentType("application/json")
         .err(true).log("Email già in uso").out(null).send();
      return;
    }
    dao.updateSelf(session.sub(), username, email, null);
    res.status(200).contentType("application/json")
       .err(false).log("Account aggiornato").out(null).send();
  }

  /**
   * DELETE /api/user/accounts/sid — cancellazione soft self. Richiede user+.
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    session.require(Role.USER, Permission.WRITE);
    new AccountDAO(db).softDelete(session.sub());
    res.status(200).contentType("application/json")
       .err(false).log("Account eliminato").out(null).send();
  }

  /**
   * POST /api/user/root — creazione account root. Non richiede autenticazione.
   * <p>
   * Operazione una tantum: crea l'account con username fisso "root" e ruolo "root".
   * Restituisce errore se l'account root esiste già.
   * </p>
   */
  public void createRoot(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String password;
    String email;
    String sql;
    List<HashMap<String, Object>> existing;
    String passwordHash;
    int rows;

    body     = req.body();
    password = (String) body.get("password");
    email    = (String) body.get("email");

    Validator.required(password, "password");
    Validator.required(email,    "email");

    sql      = "SELECT id FROM jms_accounts WHERE username = ?";
    existing = db.select(sql, ROOT_USERNAME);

    if (!existing.isEmpty()) {
      res.status(200).contentType("application/json")
         .err(true).log("Account root già esistente").out(null).send();
      return;
    }

    passwordHash = Auth.hashPassword(password);
    sql          = "INSERT INTO jms_accounts (username, email, password_hash, ruolo, must_change_password) " +
                   "VALUES (?, ?, ?, 'root', false)";
    rows         = db.query(sql, ROOT_USERNAME, email, passwordHash);

    if (rows > 0) {
      res.status(200).contentType("application/json")
         .err(false).log("Account root creato con successo").out(null).send();
    } else {
      res.status(200).contentType("application/json")
         .err(true).log("Errore nella creazione dell'account root").out(null).send();
    }
  }
}
