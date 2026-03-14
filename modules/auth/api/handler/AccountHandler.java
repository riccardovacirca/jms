package {{APP_PACKAGE}}.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;

/**
 * Impostazioni del proprio account (self-service).
 *
 * <p>GET    /api/auth/account — restituisce i dati del proprio account.
 * <p>PUT    /api/auth/account — aggiorna username, email e opzionalmente la password.
 * <p>DELETE /api/auth/account — soft delete del proprio account (imposta attivo = false).
 */
public class AccountHandler implements Handler
{
  private static final Log log = Log.get(AccountHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long selfId;
    AccountDAO dao;
    HashMap<String, Object> account;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("GET /api/auth/account: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt     = Auth.get().verifyAccessToken(token);
        selfId  = Long.parseLong(jwt.getSubject());
        dao     = new AccountDAO(db);
        account = dao.findByIdManagement(selfId);
        res.status(200)
           .contentType("application/json")
           .err(false).log(null).out(account)
           .send();
      } catch (JWTVerificationException e) {
        log.warn("GET /api/auth/account: token non valido");
        res.status(200)
           .contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null)
           .send();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long selfId;
    HashMap<String, Object> body;
    String username;
    String email;
    String password;
    String policyError;
    String passwordHash;
    AccountDAO dao;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("PUT /api/auth/account: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt    = Auth.get().verifyAccessToken(token);
        selfId = Long.parseLong(jwt.getSubject());
        try {
          body     = Json.decode(req.getBody(), HashMap.class);
          username = Validator.required((String) body.get("username"), "username");
          email    = (String) body.get("email");
          password = (String) body.get("password");

          if (password != null && !password.isBlank()) {
            policyError = Auth.validatePassword(password);
            if (policyError != null) {
              throw new ValidationException(policyError);
            }
            passwordHash = Auth.hashPassword(password);
          } else {
            passwordHash = null;
          }

          dao = new AccountDAO(db);
          if (dao.existsByUsername(username, selfId)) {
            throw new ValidationException("Username già in uso");
          }
          if (dao.existsByEmail(email, selfId)) {
            throw new ValidationException("Email già in uso");
          }

          dao.updateSelf(selfId, username, email, passwordHash);
          log.info("Account aggiornato: id={}", selfId);
          res.status(200)
             .contentType("application/json")
             .err(false).log(null).out(null)
             .send();
        } catch (ValidationException e) {
          log.warn("PUT /api/auth/account: {}", e.getMessage());
          res.status(200)
             .contentType("application/json")
             .err(true).log(e.getMessage()).out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("PUT /api/auth/account: token non valido");
        res.status(200)
           .contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null)
           .send();
      }
    }
  }

  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long selfId;
    AccountDAO dao;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("DELETE /api/auth/account: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt    = Auth.get().verifyAccessToken(token);
        selfId = Long.parseLong(jwt.getSubject());
        dao    = new AccountDAO(db);
        dao.softDelete(selfId);
        log.info("Account disattivato: id={}", selfId);
        res.status(200)
           .contentType("application/json")
           .err(false).log(null).out(null)
           .send();
      } catch (JWTVerificationException e) {
        log.warn("DELETE /api/auth/account: token non valido");
        res.status(200)
           .contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null)
           .send();
      }
    }
  }
}
