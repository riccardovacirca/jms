package dev.jms.app.user.handler;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.helper.AccountSearchHelper;
import dev.jms.app.user.helper.PasswordChangeHelper;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per le operazioni CRUD sull'entità Account.
 *
 * <p>Rotte gestite (registrate in Routes.java):
 * <ul>
 *   <li>GET    /api/user/accounts                - lista paginata (admin+)</li>
 *   <li>GET    /api/user/accounts/{id}           - account per id (admin+)</li>
 *   <li>GET    /api/user/accounts/sid            - account in sessione (user+)</li>
 *   <li>POST   /api/user/accounts                - crea account (nessuna auth per user, root per admin)</li>
 *   <li>POST   /api/user/root                    - creazione account root una tantum (nessuna auth)</li>
 *   <li>PUT    /api/user/accounts/{id}           - aggiornamento (self: user+, username/email; admin: admin+, campi completi)</li>
 *   <li>PUT    /api/user/accounts/{id}/password  - cambio password (self: verifica corrente; admin: reset diretto)</li>
 *   <li>DELETE /api/user/accounts/{id}           - cancellazione soft (self: user+; admin: admin+ con gerarchia)</li>
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

    if ("admin".equals(ruolo)) {
      session.require(Role.ROOT, Permission.WRITE);
    } else if (!"user".equals(ruolo)) {
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
   * PUT /api/user/accounts/{id} — aggiornamento account.
   * <p>Se {@code id} corrisponde all'account in sessione aggiorna solo username ed email (user+).
   * Altrimenti richiede admin+ e consente di modificare anche ruolo, attivo e must_change_password.</p>
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    HashMap<String, Object> existing;
    String username;
    String email;
    String ruolo;
    String existingRuolo;
    boolean attivo;
    boolean mustChangePassword;
    long id;
    boolean isSelf;
    AccountDAO dao;

    id       = Long.parseLong(req.urlArgs().get("id"));
    body     = req.body();
    username = (String) body.get("username");
    email    = (String) body.get("email");
    dao      = new AccountDAO(db);
    isSelf   = id == session.sub();

    Validator.required(username, "username");

    if (isSelf) {
      session.require(Role.USER, Permission.WRITE);
      if (dao.existsByUsername(username, id)) {
        res.status(200).contentType("application/json")
           .err(true).log("Username già in uso").out(null).send();
        return;
      }
      if (dao.existsByEmail(email, id)) {
        res.status(200).contentType("application/json")
           .err(true).log("Email già in uso").out(null).send();
        return;
      }
      dao.updateSelf(id, username, email, null);
    } else {
      session.require(Role.ADMIN, Permission.WRITE);
      ruolo              = (String) body.get("ruolo");
      attivo             = Boolean.TRUE.equals(body.get("attivo"));
      mustChangePassword = Boolean.TRUE.equals(body.get("must_change_password"));
      Validator.required(ruolo, "ruolo");
      existing = dao.findByIdManagement(id);
      if (existing == null) {
        res.status(200).contentType("application/json")
           .err(true).log("Account non trovato").out(null).send();
        return;
      }
      existingRuolo = (String) existing.get("ruolo");
      if ("root".equals(existingRuolo)) {
        res.status(200).contentType("application/json")
           .err(true).log("L'account root non può essere modificato").out(null).send();
        return;
      }
      if ("admin".equals(existingRuolo) && session.ruoloLevel() < 3) {
        res.status(200).contentType("application/json")
           .err(true).log("Solo root può modificare un account admin").out(null).send();
        return;
      }
      if (!"user".equals(ruolo) && !"admin".equals(ruolo)) {
        res.status(200).contentType("application/json")
           .err(true).log("Ruolo non valido. Valori accettati: user, admin").out(null).send();
        return;
      }
      if ("admin".equals(ruolo) && session.ruoloLevel() < 3) {
        res.status(200).contentType("application/json")
           .err(true).log("Solo root può assegnare il ruolo admin").out(null).send();
        return;
      }
      if (dao.existsByUsername(username, id)) {
        res.status(200).contentType("application/json")
           .err(true).log("Username già in uso").out(null).send();
        return;
      }
      if (dao.existsByEmail(email, id)) {
        res.status(200).contentType("application/json")
           .err(true).log("Email già in uso").out(null).send();
        return;
      }
      dao.adminUpdate(id, username, email, ruolo, attivo, mustChangePassword);
    }
    res.status(200).contentType("application/json")
       .err(false).log("Account aggiornato").out(null).send();
  }

  /**
   * PUT /api/user/accounts/{id}/password — cambio password.
   * <p>Se {@code id} corrisponde all'account in sessione verifica la password corrente (user+).
   * Altrimenti richiede admin+ e imposta la nuova password con must_change_password=true.</p>
   */
  public void changePassword(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    HashMap<String, Object> existing;
    String newPassword;
    String currentPassword;
    String existingRuolo;
    long id;
    boolean isSelf;
    AccountDAO dao;

    id          = Long.parseLong(req.urlArgs().get("id"));
    body        = req.body();
    newPassword = (String) body.get("new_password");
    dao         = new AccountDAO(db);
    isSelf      = id == session.sub();

    Validator.required(newPassword, "new_password");

    if (isSelf) {
      session.require(Role.USER, Permission.WRITE);
      currentPassword = (String) body.get("current_password");
      Validator.required(currentPassword, "current_password");
      try {
        PasswordChangeHelper.changePassword(db, (int) id, currentPassword, newPassword);
      } catch (ValidationException e) {
        res.status(200).contentType("application/json")
           .err(true).log(e.getMessage()).out(null).send();
        return;
      }
    } else {
      session.require(Role.ADMIN, Permission.WRITE);
      existing = dao.findByIdManagement(id);
      if (existing == null) {
        res.status(200).contentType("application/json")
           .err(true).log("Account non trovato").out(null).send();
        return;
      }
      existingRuolo = (String) existing.get("ruolo");
      if ("root".equals(existingRuolo)) {
        res.status(200).contentType("application/json")
           .err(true).log("La password dell'account root non può essere resettata").out(null).send();
        return;
      }
      if ("admin".equals(existingRuolo) && session.ruoloLevel() < 3) {
        res.status(200).contentType("application/json")
           .err(true).log("Solo root può resettare la password di un account admin").out(null).send();
        return;
      }
      dao.updatePassword((int) id, Auth.hashPassword(newPassword), true);
    }
    res.status(200).contentType("application/json")
       .err(false).log("Password aggiornata").out(null).send();
  }

  /**
   * DELETE /api/user/accounts/{id} — cancellazione soft.
   * <p>Se {@code id} corrisponde all'account in sessione elimina il proprio account (user+).
   * Altrimenti richiede admin+ con controllo della gerarchia dei ruoli.</p>
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> existing;
    String existingRuolo;
    long id;
    boolean isSelf;
    AccountDAO dao;

    id     = Long.parseLong(req.urlArgs().get("id"));
    dao    = new AccountDAO(db);
    isSelf = id == session.sub();

    if (isSelf) {
      session.require(Role.USER, Permission.WRITE);
      dao.softDelete(id);
    } else {
      session.require(Role.ADMIN, Permission.WRITE);
      existing = dao.findByIdManagement(id);
      if (existing == null) {
        res.status(200).contentType("application/json")
           .err(true).log("Account non trovato").out(null).send();
        return;
      }
      existingRuolo = (String) existing.get("ruolo");
      if ("root".equals(existingRuolo)) {
        res.status(200).contentType("application/json")
           .err(true).log("L'account root non può essere eliminato").out(null).send();
        return;
      }
      if ("admin".equals(existingRuolo) && session.ruoloLevel() < 3) {
        res.status(200).contentType("application/json")
           .err(true).log("Solo root può eliminare un account admin").out(null).send();
        return;
      }
      dao.softDelete(id);
    }
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

    sql      = "SELECT id FROM jms_user_accounts WHERE username = ?";
    existing = db.select(sql, ROOT_USERNAME);

    if (!existing.isEmpty()) {
      res.status(200).contentType("application/json")
         .err(true).log("Account root già esistente").out(null).send();
      return;
    }

    passwordHash = Auth.hashPassword(password);
    sql          = "INSERT INTO jms_user_accounts (username, email, password_hash, ruolo, must_change_password) " +
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
