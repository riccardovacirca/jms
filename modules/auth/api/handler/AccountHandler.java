package {{APP_PACKAGE}}.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.Mail;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;
import java.util.List;

/**
 * Gestione account utenti via REST.
 *
 * GET    /api/auth/account        — lista: admin vede tutti, operatore solo sé stesso.
 * POST   /api/auth/account        — crea account (solo admin).
 * GET    /api/auth/account/{id}   — dettaglio: admin vede tutti, operatore solo sé stesso.
 * PUT    /api/auth/account/{id}   — modifica: admin modifica tutto, operatore solo i propri dati.
 * DELETE /api/auth/account/{id}   — soft delete (solo admin; protegge l'ultimo admin attivo).
 */
public class AccountHandler implements Handler
{
  private static final Log log = Log.get(AccountHandler.class);

  private final String baseUrl;

  public AccountHandler(Config config)
  {
    this.baseUrl = config.get("app.base.url", "http://localhost:2310");
  }

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    long selfId;
    String idParam;
    AccountDAO dao;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("GET account: token assente");
      res.status(200).contentType("application/json")
         .err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        idParam  = req.urlArgs().get("id");
        dao      = new AccountDAO(db);
        if (idParam == null) {
          getList(req, res, db, dao, ruolo, selfId);
        } else {
          getItem(res, dao, ruolo, selfId, Long.parseLong(idParam));
        }
      } catch (JWTVerificationException e) {
        log.warn("GET account: token non valido");
        res.status(200).contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    List<String> permissions;
    HashMap<String, Object> body;
    String username;
    String email;
    String password;
    String newRuolo;
    boolean sendNotification;
    String policyError;
    String passwordHash;
    AccountDAO dao;
    long newId;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Creazione account: token assente");
      res.status(200).contentType("application/json")
         .err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt         = Auth.get().verifyAccessToken(token);
        ruolo       = jwt.getClaim("ruolo").asString();
        permissions = jwt.getClaim("permissions").asList(String.class);
        if (!"admin".equals(ruolo)) {
          log.warn("Creazione account: accesso negato per ruolo '{}'", ruolo);
          res.status(200).contentType("application/json")
             .err(true).log("Accesso negato").out(null).send();
        } else {
          try {
            body             = Json.decode(req.getBody(), HashMap.class);
            username         = Validator.required((String) body.get("username"), "username");
            email            = (String) body.get("email");
            password         = Validator.required((String) body.get("password"), "password");
            newRuolo         = Validator.required((String) body.get("ruolo"), "ruolo");
            sendNotification = Boolean.TRUE.equals(body.get("send_notification"));

            policyError = Auth.validatePassword(password);
            if (policyError != null) {
              throw new ValidationException(policyError);
            }

            dao = new AccountDAO(db);
            if (dao.existsByUsername(username, null)) {
              throw new ValidationException("Username già in uso");
            }
            if (dao.existsByEmail(email, null)) {
              throw new ValidationException("Email già in uso");
            }

            passwordHash = Auth.hashPassword(password);
            newId        = dao.createAccount(username, email, passwordHash, newRuolo);
            log.info("Account creato: username='{}', ruolo='{}', id={}", username, newRuolo, newId);

            if (sendNotification
                && permissions != null && permissions.contains("can_send_mail")
                && email != null && !email.isBlank()
                && Mail.isConfigured()) {
              Mail.get().send(
                email,
                "Benvenuto — il tuo account è stato creato",
                "Ciao " + username + ",\n\n" +
                "Il tuo account è stato creato con le seguenti credenziali:\n" +
                "  Username: " + username + "\n" +
                "  Password: " + password + "\n\n" +
                "Al primo accesso ti verrà chiesto di modificare la password.\n\n" +
                "Accedi su: " + baseUrl
              );
              log.info("Email di benvenuto inviata a '{}' per accountId {}", email, newId);
            }

            out = new HashMap<>();
            out.put("id", newId);
            res.status(200).contentType("application/json")
               .err(false).log(null).out(out).send();
          } catch (ValidationException e) {
            log.warn("Creazione account fallita: {}", e.getMessage());
            res.status(200).contentType("application/json")
               .err(true).log(e.getMessage()).out(null).send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Creazione account: token non valido");
        res.status(200).contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    long selfId;
    long targetId;
    HashMap<String, Object> body;
    String username;
    String email;
    String password;
    String policyError;
    String passwordHash;
    String targetRuolo;
    boolean attivo;
    AccountDAO dao;
    HashMap<String, Object> existing;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Modifica account: token assente");
      res.status(200).contentType("application/json")
         .err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        targetId = Long.parseLong(req.urlArgs().get("id"));
        if (!"admin".equals(ruolo) && selfId != targetId) {
          log.warn("Modifica account {}: accesso negato per accountId {}", targetId, selfId);
          res.status(200).contentType("application/json")
             .err(true).log("Accesso negato").out(null).send();
        } else {
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

            dao      = new AccountDAO(db);
            existing = dao.findByIdManagement(targetId);
            if (existing == null) {
              throw new ValidationException("Account non trovato");
            }
            if (dao.existsByUsername(username, targetId)) {
              throw new ValidationException("Username già in uso");
            }
            if (dao.existsByEmail(email, targetId)) {
              throw new ValidationException("Email già in uso");
            }

            if ("admin".equals(ruolo)) {
              targetRuolo = body.containsKey("ruolo")
                ? (String) body.get("ruolo")
                : DB.toString(existing.get("ruolo"));
              attivo = body.containsKey("attivo")
                ? Boolean.TRUE.equals(body.get("attivo"))
                : Boolean.TRUE.equals(existing.get("attivo"));
              dao.updateAccount(targetId, username, email, passwordHash, targetRuolo, attivo);
            } else {
              dao.updateSelf(targetId, username, email, passwordHash);
            }

            log.info("Account {} modificato da accountId {}", targetId, selfId);
            res.status(200).contentType("application/json")
               .err(false).log(null).out(null).send();
          } catch (ValidationException e) {
            log.warn("Modifica account fallita: {}", e.getMessage());
            res.status(200).contentType("application/json")
               .err(true).log(e.getMessage()).out(null).send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Modifica account: token non valido");
        res.status(200).contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }

  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    long selfId;
    long targetId;
    AccountDAO dao;
    HashMap<String, Object> existing;
    long activeAdminCount;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Disattiva account: token assente");
      res.status(200).contentType("application/json")
         .err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        targetId = Long.parseLong(req.urlArgs().get("id"));
        if (!"admin".equals(ruolo)) {
          log.warn("Disattiva account {}: accesso negato per ruolo '{}'", targetId, ruolo);
          res.status(200).contentType("application/json")
             .err(true).log("Accesso negato").out(null).send();
        } else {
          dao      = new AccountDAO(db);
          existing = dao.findByIdManagement(targetId);
          if (existing == null) {
            res.status(200).contentType("application/json")
               .err(true).log("Account non trovato").out(null).send();
          } else {
            activeAdminCount = dao.countActiveByRole("admin");
            if ("admin".equals(DB.toString(existing.get("ruolo"))) && activeAdminCount <= 1) {
              log.warn("Disattiva account {}: impossibile disattivare l'ultimo admin attivo", targetId);
              res.status(200).contentType("application/json")
                 .err(true).log("Impossibile disattivare l'ultimo amministratore attivo").out(null).send();
            } else {
              dao.softDelete(targetId);
              log.info("Account {} disattivato da adminId {}", targetId, selfId);
              res.status(200).contentType("application/json")
                 .err(false).log(null).out(null).send();
            }
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Disattiva account: token non valido");
        res.status(200).contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }

  // -------------------------
  // helpers privati
  // -------------------------

  private void getList(HttpRequest req, HttpResponse res, DB db,
                       AccountDAO dao, String ruolo, long selfId) throws Exception
  {
    String search;
    HashMap<String, Object> self;
    List<HashMap<String, Object>> accounts;

    if ("admin".equals(ruolo)) {
      search   = req.getQueryParam("q");
      accounts = dao.findAllManagement(search);
    } else {
      self     = dao.findByIdManagement(selfId);
      accounts = self != null ? List.of(self) : List.of();
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(accounts).send();
  }

  private void getItem(HttpResponse res, AccountDAO dao,
                       String ruolo, long selfId, long targetId) throws Exception
  {
    HashMap<String, Object> account;

    if (!"admin".equals(ruolo) && selfId != targetId) {
      log.warn("Dettaglio account {}: accesso negato per accountId {}", targetId, selfId);
      res.status(200).contentType("application/json")
         .err(true).log("Accesso negato").out(null).send();
    } else {
      account = dao.findByIdManagement(targetId);
      if (account == null) {
        res.status(200).contentType("application/json")
           .err(true).log("Account non trovato").out(null).send();
      } else {
        res.status(200).contentType("application/json")
           .err(false).log(null).out(account).send();
      }
    }
  }
}
