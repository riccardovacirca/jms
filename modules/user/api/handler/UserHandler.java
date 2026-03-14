package {{APP_PACKAGE}}.user.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.user.dao.UserDAO;
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
 * Profilo utente (self-service).
 *
 * <p>GET    /api/user — restituisce il proprio profilo (null se non ancora creato).
 * <p>POST   /api/user — crea il proprio profilo (account_id dalla sessione JWT).
 * <p>PUT    /api/user — aggiorna il proprio profilo.
 * <p>DELETE /api/user — soft delete del proprio profilo (imposta attivo = false).
 */
public class UserHandler implements Handler
{
  private static final Log log = Log.get(UserHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long accountId;
    UserDAO dao;
    HashMap<String, Object> profile;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("GET /api/user: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        accountId = Long.parseLong(jwt.getSubject());
        dao       = new UserDAO(db);
        profile   = dao.findByAccountId(accountId);
        res.status(200)
           .contentType("application/json")
           .err(false).log(null).out(profile)
           .send();
      } catch (JWTVerificationException e) {
        log.warn("GET /api/user: token non valido");
        res.status(200)
           .contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null)
           .send();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long accountId;
    HashMap<String, Object> body;
    String nome;
    String cognome;
    String nickname;
    String immagine;
    int flags;
    UserDAO dao;
    long newId;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("POST /api/user: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        accountId = Long.parseLong(jwt.getSubject());
        try {
          body     = Json.decode(req.getBody(), HashMap.class);
          nome     = Validator.required((String) body.get("nome"), "nome");
          cognome  = Validator.required((String) body.get("cognome"), "cognome");
          nickname = (String) body.get("nickname");
          immagine = (String) body.get("immagine");
          flags    = body.get("flags") != null ? ((Number) body.get("flags")).intValue() : 0;
          dao      = new UserDAO(db);
          if (dao.existsByAccountId(accountId)) {
            throw new ValidationException("Profilo già esistente per questo account");
          }
          if (dao.existsByNickname(nickname, null)) {
            throw new ValidationException("Nickname già in uso");
          }
          newId = dao.create(accountId, nome, cognome, nickname, immagine, flags);
          log.info("Profilo utente creato: accountId={}, userId={}", accountId, newId);
          out = new HashMap<>();
          out.put("id", newId);
          res.status(200)
             .contentType("application/json")
             .err(false).log(null).out(out)
             .send();
        } catch (ValidationException e) {
          log.warn("POST /api/user: {}", e.getMessage());
          res.status(200)
             .contentType("application/json")
             .err(true).log(e.getMessage()).out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("POST /api/user: token non valido");
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
    long accountId;
    HashMap<String, Object> body;
    String nome;
    String cognome;
    String nickname;
    String immagine;
    int flags;
    UserDAO dao;
    HashMap<String, Object> existing;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("PUT /api/user: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        accountId = Long.parseLong(jwt.getSubject());
        try {
          dao      = new UserDAO(db);
          existing = dao.findByAccountId(accountId);
          if (existing == null) {
            throw new ValidationException("Profilo non trovato");
          }
          body     = Json.decode(req.getBody(), HashMap.class);
          nome     = Validator.required((String) body.get("nome"), "nome");
          cognome  = Validator.required((String) body.get("cognome"), "cognome");
          nickname = (String) body.get("nickname");
          immagine = (String) body.get("immagine");
          flags    = body.get("flags") != null ? ((Number) body.get("flags")).intValue() : 0;
          if (dao.existsByNickname(nickname, DB.toLong(existing.get("id")))) {
            throw new ValidationException("Nickname già in uso");
          }
          dao.update(DB.toLong(existing.get("id")), nome, cognome, nickname, immagine, flags,
                     DB.toBoolean(existing.get("attivo")));
          log.info("Profilo utente aggiornato: accountId={}", accountId);
          res.status(200)
             .contentType("application/json")
             .err(false).log(null).out(null)
             .send();
        } catch (ValidationException e) {
          log.warn("PUT /api/user: {}", e.getMessage());
          res.status(200)
             .contentType("application/json")
             .err(true).log(e.getMessage()).out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("PUT /api/user: token non valido");
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
    long accountId;
    UserDAO dao;
    HashMap<String, Object> existing;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("DELETE /api/user: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        accountId = Long.parseLong(jwt.getSubject());
        dao       = new UserDAO(db);
        existing  = dao.findByAccountId(accountId);
        if (existing == null) {
          res.status(200)
             .contentType("application/json")
             .err(true).log("Profilo non trovato").out(null)
             .send();
        } else {
          dao.softDelete(DB.toLong(existing.get("id")));
          log.info("Profilo utente disattivato: accountId={}", accountId);
          res.status(200)
             .contentType("application/json")
             .err(false).log(null).out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("DELETE /api/user: token non valido");
        res.status(200)
           .contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null)
           .send();
      }
    }
  }
}
