package {{APP_PACKAGE}}.account.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.account.dao.UserDAO;
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
 * GET    /api/users/{id} — dettaglio: admin vede tutti, operatore solo sé stesso.
 * PUT    /api/users/{id} — modifica: admin cambia tutti i campi, operatore solo i propri dati.
 * DELETE /api/users/{id} — elimina (solo admin; impossibile eliminare l'ultimo admin).
 */
public class UserItemHandler implements Handler
{
  private static final Log log = Log.get(UserItemHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String ruolo;
    long selfId;
    long targetId;
    UserDAO dao;
    HashMap<String, Object> account;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Dettaglio utente: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        targetId = Long.parseLong(req.urlArgs().get("id"));
        if (!"admin".equals(ruolo) && selfId != targetId) {
          log.warn("Dettaglio utente {}: accesso negato per accountId {}", targetId, selfId);
          res.status(200).contentType("application/json").err(true).log("Accesso negato").out(null).send();
        } else {
          dao     = new UserDAO(db);
          account = dao.findById(targetId);
          if (account == null) {
            res.status(200).contentType("application/json").err(true).log("Utente non trovato").out(null).send();
          } else {
            res.status(200).contentType("application/json").err(false).log(null).out(account).send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Dettaglio utente: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
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
    UserDAO dao;
    HashMap<String, Object> existing;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Modifica utente: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        targetId = Long.parseLong(req.urlArgs().get("id"));
        if (!"admin".equals(ruolo) && selfId != targetId) {
          log.warn("Modifica utente {}: accesso negato per accountId {}", targetId, selfId);
          res.status(200).contentType("application/json").err(true).log("Accesso negato").out(null).send();
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

            dao      = new UserDAO(db);
            existing = dao.findById(targetId);
            if (existing == null) {
              throw new ValidationException("Utente non trovato");
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
              dao.update(targetId, username, email, passwordHash, targetRuolo, attivo);
            } else {
              dao.updateSelf(targetId, username, email, passwordHash);
            }

            log.info("Account {} aggiornato da accountId {}", targetId, selfId);
            res.status(200).contentType("application/json").err(false).log(null).out(null).send();
          } catch (ValidationException e) {
            log.warn("Modifica utente fallita: {}", e.getMessage());
            res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Modifica utente: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
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
    UserDAO dao;
    HashMap<String, Object> existing;
    long adminCount;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Elimina utente: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        ruolo    = jwt.getClaim("ruolo").asString();
        selfId   = Long.parseLong(jwt.getSubject());
        targetId = Long.parseLong(req.urlArgs().get("id"));
        if (!"admin".equals(ruolo)) {
          log.warn("Elimina utente {}: accesso negato per ruolo '{}'", targetId, ruolo);
          res.status(200).contentType("application/json").err(true).log("Accesso negato").out(null).send();
        } else {
          dao      = new UserDAO(db);
          existing = dao.findById(targetId);
          if (existing == null) {
            res.status(200).contentType("application/json").err(true).log("Utente non trovato").out(null).send();
          } else {
            adminCount = dao.countByRole("admin");
            if ("admin".equals(DB.toString(existing.get("ruolo"))) && adminCount <= 1) {
              log.warn("Elimina utente {}: impossibile eliminare l'ultimo admin", targetId);
              res.status(200).contentType("application/json")
                 .err(true).log("Impossibile eliminare l'ultimo amministratore").out(null).send();
            } else {
              dao.delete(targetId);
              log.info("Account {} eliminato da adminId {}", targetId, selfId);
              res.status(200).contentType("application/json").err(false).log(null).out(null).send();
            }
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("Elimina utente: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
