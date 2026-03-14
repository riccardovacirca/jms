package {{APP_PACKAGE}}.user.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.user.dao.UserDAO;
import {{APP_PACKAGE}}.user.dao.UserSettingsDAO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.HashMap;
import java.util.List;

/**
 * Handler REST per le impostazioni utente (chiave/valore).
 * Le impostazioni sono sempre relative al profilo dell'utente autenticato.
 *
 * <p>GET    /api/user/settings       — lista tutte le impostazioni.
 * <p>GET    /api/user/settings/{key} — singola impostazione.
 * <p>PUT    /api/user/settings/{key} — inserisce o aggiorna un'impostazione.
 * <p>DELETE /api/user/settings/{key} — elimina un'impostazione.
 */
public class UserSettingsHandler implements Handler
{
  private static final Log log = Log.get(UserSettingsHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long accountId;
    String key;
    UserDAO userDao;
    UserSettingsDAO settingsDao;
    HashMap<String, Object> profile;
    long userId;
    List<HashMap<String, Object>> all;
    HashMap<String, Object> one;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("GET /api/user/settings: token assente");
      res.status(200)
         .contentType("application/json")
         .err(true).log("Non autenticato").out(null)
         .send();
    } else {
      try {
        jwt       = Auth.get().verifyAccessToken(token);
        accountId = Long.parseLong(jwt.getSubject());
        userDao   = new UserDAO(db);
        profile   = userDao.findByAccountId(accountId);
        if (profile == null) {
          res.status(200)
             .contentType("application/json")
             .err(true).log("Profilo non trovato").out(null)
             .send();
        } else {
          userId      = DB.toLong(profile.get("id"));
          settingsDao = new UserSettingsDAO(db);
          key         = req.urlArgs().get("key");
          if (key != null) {
            one = settingsDao.findByKey(userId, key);
            res.status(200)
               .contentType("application/json")
               .err(false).log(null).out(one)
               .send();
          } else {
            all = settingsDao.findAllByUserId(userId);
            res.status(200)
               .contentType("application/json")
               .err(false).log(null).out(all)
               .send();
          }
        }
      } catch (JWTVerificationException e) {
        log.warn("GET /api/user/settings: token non valido");
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
    String key;
    HashMap<String, Object> body;
    String valore;
    UserDAO userDao;
    UserSettingsDAO settingsDao;
    HashMap<String, Object> profile;
    long userId;

    key = req.urlArgs().get("key");
    if (key == null || key.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true).log("Chiave mancante").out(null)
         .send();
    } else {
      token = req.getCookie("access_token");
      if (token == null) {
        log.warn("PUT /api/user/settings/{}: token assente", key);
        res.status(200)
           .contentType("application/json")
           .err(true).log("Non autenticato").out(null)
           .send();
      } else {
        try {
          jwt       = Auth.get().verifyAccessToken(token);
          accountId = Long.parseLong(jwt.getSubject());
          userDao   = new UserDAO(db);
          profile   = userDao.findByAccountId(accountId);
          if (profile == null) {
            res.status(200)
               .contentType("application/json")
               .err(true).log("Profilo non trovato").out(null)
               .send();
          } else {
            userId      = DB.toLong(profile.get("id"));
            body        = Json.decode(req.getBody(), HashMap.class);
            valore      = (String) body.get("valore");
            settingsDao = new UserSettingsDAO(db);
            settingsDao.upsert(userId, key, valore);
            log.info("Setting aggiornato: userId={}, chiave={}", userId, key);
            res.status(200)
               .contentType("application/json")
               .err(false).log(null).out(null)
               .send();
          }
        } catch (JWTVerificationException e) {
          log.warn("PUT /api/user/settings/{}: token non valido", key);
          res.status(200)
             .contentType("application/json")
             .err(true).log("Token non valido o scaduto").out(null)
             .send();
        }
      }
    }
  }

  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    long accountId;
    String key;
    UserDAO userDao;
    UserSettingsDAO settingsDao;
    HashMap<String, Object> profile;
    long userId;

    key = req.urlArgs().get("key");
    if (key == null || key.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true).log("Chiave mancante").out(null)
         .send();
    } else {
      token = req.getCookie("access_token");
      if (token == null) {
        log.warn("DELETE /api/user/settings/{}: token assente", key);
        res.status(200)
           .contentType("application/json")
           .err(true).log("Non autenticato").out(null)
           .send();
      } else {
        try {
          jwt       = Auth.get().verifyAccessToken(token);
          accountId = Long.parseLong(jwt.getSubject());
          userDao   = new UserDAO(db);
          profile   = userDao.findByAccountId(accountId);
          if (profile == null) {
            res.status(200)
               .contentType("application/json")
               .err(true).log("Profilo non trovato").out(null)
               .send();
          } else {
            userId      = DB.toLong(profile.get("id"));
            settingsDao = new UserSettingsDAO(db);
            settingsDao.delete(userId, key);
            log.info("Setting eliminato: userId={}, chiave={}", userId, key);
            res.status(200)
               .contentType("application/json")
               .err(false).log(null).out(null)
               .send();
          }
        } catch (JWTVerificationException e) {
          log.warn("DELETE /api/user/settings/{}: token non valido", key);
          res.status(200)
             .contentType("application/json")
             .err(true).log("Token non valido o scaduto").out(null)
             .send();
        }
      }
    }
  }
}
