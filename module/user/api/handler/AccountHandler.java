package dev.jms.app.user.handler;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.helper.AccountSearchHelper;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Validator;

import java.util.HashMap;
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
 *   <li>PUT    /api/user/accounts/sid       - aggiornamento self</li>
 *   <li>DELETE /api/user/accounts/sid       - cancellazione soft self</li>
 * </ul>
 */
public class AccountHandler
{
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
}
