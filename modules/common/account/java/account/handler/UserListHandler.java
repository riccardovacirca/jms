package {{APP_PACKAGE}}.account.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.account.dao.UserDAO;
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
 * GET  /api/users — lista utenti: admin vede tutti, operatore vede solo sé stesso.
 * POST /api/users — crea utente (solo admin).
 */
public class UserListHandler implements Handler
{
  private static final Log log = Log.get(UserListHandler.class);

  private final String baseUrl;

  public UserListHandler(Config config)
  {
    this.baseUrl = config.get("app.base.url", "http://localhost:2310");
  }

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    long accountId;
    String search;
    UserDAO dao;
    HashMap<String, Object> self;
    List<HashMap<String, Object>> users;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Lista utenti: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        ruolo     = jwt.getClaim("ruolo").asString();
        accountId = Long.parseLong(jwt.getSubject());
        search    = req.getQueryParam("q");
        dao       = new UserDAO(db);
        if ("admin".equals(ruolo)) {
          users = dao.findAll(search);
        } else {
          self  = dao.findById(accountId);
          users = self != null ? List.of(self) : List.of();
        }
        res.status(200).contentType("application/json").err(false).log(null).out(users).send();
      } catch (JWTVerificationException e) {
        log.warn("Lista utenti: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
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
    UserDAO dao;
    long newId;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Creazione utente: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt         = Auth.get().verifyAccessToken(token);
        ruolo       = jwt.getClaim("ruolo").asString();
        permissions = jwt.getClaim("permissions").asList(String.class);
        if (!"admin".equals(ruolo)) {
          log.warn("Creazione utente: accesso negato per ruolo '{}'", ruolo);
          res.status(200).contentType("application/json").err(true).log("Accesso negato").out(null).send();
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

            dao = new UserDAO(db);
            if (dao.existsByUsername(username, null)) {
              throw new ValidationException("Username già in uso");
            }
            if (dao.existsByEmail(email, null)) {
              throw new ValidationException("Email già in uso");
            }

            passwordHash = Auth.hashPassword(password);
            newId        = dao.create(username, email, passwordHash, newRuolo);
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
            res.status(200).contentType("application/json").err(false).log(null).out(out).send();
          } catch (ValidationException e) {
            log.warn("Creazione utente fallita: {}", e.getMessage());
            res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Creazione utente: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
