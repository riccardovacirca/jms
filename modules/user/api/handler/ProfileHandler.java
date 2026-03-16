package {{APP_PACKAGE}}.user.handler;

import {{APP_PACKAGE}}.user.dao.ProfileDAO;
import {{APP_PACKAGE}}.user.helper.ProfileSettingsHelper;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler per le operazioni sul profilo utente.
 *
 * <p>Rotte gestite (registrate in Routes.java):
 * <ul>
 *   <li>GET    /api/user/users/sid                     - profilo in sessione</li>
 *   <li>PUT    /api/user/users/sid                     - aggiornamento profilo</li>
 *   <li>GET    /api/user/users/sid/settings            - tutte le impostazioni</li>
 *   <li>POST   /api/user/users/sid/settings            - upsert impostazione</li>
 *   <li>GET    /api/user/users/sid/settings/{key}      - singola impostazione</li>
 *   <li>DELETE /api/user/users/sid/settings/{key}      - elimina impostazione</li>
 * </ul>
 */
public class ProfileHandler
{
  /** GET /api/user/users/sid — profilo in sessione. Richiede JWT. */
  public void sid(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims    = req.requireAuth();
    long                accountId = Long.parseLong(claims.get("sub").toString());
    HashMap<String, Object> profile = new ProfileDAO(db).findByAccountId(accountId);
    if (profile == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Profilo non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(profile).send();
  }

  /** PUT /api/user/users/sid — crea o aggiorna il profilo in sessione. Richiede JWT. */
  public void update(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims    = req.requireAuth();
    long                accountId = Long.parseLong(claims.get("sub").toString());
    HashMap<String, Object> body  = req.body();
    String nome     = (String) body.get("nome");
    String cognome  = (String) body.get("cognome");
    String nickname = (String) body.get("nickname");
    String immagine = (String) body.get("immagine");
    Validator.required(nome, "nome");
    Validator.required(cognome, "cognome");
    ProfileDAO dao    = new ProfileDAO(db);
    boolean    exists = dao.existsByAccountId(accountId);
    if (exists) {
      HashMap<String, Object> current   = dao.findByAccountId(accountId);
      long                    profileId = Long.parseLong(current.get("id").toString());
      if (nickname != null && !nickname.isBlank() && dao.existsByNickname(nickname, profileId)) {
        res.status(200).contentType("application/json")
           .err(true).log("Nickname già in uso").out(null).send();
        return;
      }
      dao.update(profileId, nome, cognome, nickname, immagine, 0, true);
    } else {
      if (nickname != null && !nickname.isBlank() && dao.existsByNickname(nickname, null)) {
        res.status(200).contentType("application/json")
           .err(true).log("Nickname già in uso").out(null).send();
        return;
      }
      dao.create(accountId, nome, cognome, nickname, immagine, 0);
    }
    res.status(200).contentType("application/json")
       .err(false).log("Profilo aggiornato").out(null).send();
  }

  /** GET /api/user/users/sid/settings — tutte le impostazioni. Richiede JWT. */
  public void settings(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object>          claims    = req.requireAuth();
    long                         accountId = Long.parseLong(claims.get("sub").toString());
    ProfileSettingsHelper        helper    = new ProfileSettingsHelper(db);
    List<HashMap<String, Object>> list     = helper.getAll(accountId);
    res.status(200).contentType("application/json")
       .err(false).log(null).out(list != null ? list : Collections.emptyList()).send();
  }

  /** POST /api/user/users/sid/settings — crea o aggiorna impostazione. Richiede JWT. */
  public void addSetting(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims    = req.requireAuth();
    long                accountId = Long.parseLong(claims.get("sub").toString());
    HashMap<String, Object> body  = req.body();
    String chiave = (String) body.get("chiave");
    String valore = (String) body.get("valore");
    Validator.required(chiave, "chiave");
    Validator.required(valore, "valore");
    ProfileSettingsHelper helper = new ProfileSettingsHelper(db);
    if (!helper.upsert(accountId, chiave, valore)) {
      res.status(200).contentType("application/json")
         .err(true).log("Profilo non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log("Impostazione salvata").out(null).send();
  }

  /** GET /api/user/users/sid/settings/{key} — singola impostazione. Richiede JWT. */
  public void settingByKey(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object>          claims    = req.requireAuth();
    long                         accountId = Long.parseLong(claims.get("sub").toString());
    String                       key       = req.urlArgs().get("key");
    ProfileSettingsHelper        helper    = new ProfileSettingsHelper(db);
    List<HashMap<String, Object>> all      = helper.getAll(accountId);
    HashMap<String, Object>      found     = null;
    if (all != null) {
      for (HashMap<String, Object> s : all) {
        if (key.equals(s.get("chiave"))) {
          found = s;
          break;
        }
      }
    }
    if (found == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Impostazione non trovata").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(found).send();
  }

  /** DELETE /api/user/users/sid/settings/{key} — elimina impostazione. Richiede JWT. */
  public void deleteSetting(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims    = req.requireAuth();
    long                accountId = Long.parseLong(claims.get("sub").toString());
    String              key       = req.urlArgs().get("key");
    new ProfileSettingsHelper(db).delete(accountId, key);
    res.status(200).contentType("application/json")
       .err(false).log("Impostazione eliminata").out(null).send();
  }
}
