package dev.jms.app.user.handler;

import dev.jms.app.user.dao.ProfileDAO;
import dev.jms.app.user.helper.ProfileSettingsHelper;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.Validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
  /** GET /api/user/users/sid — profilo in sessione. Richiede user+. */
  public void sid(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> profile;

    session.require(Role.USER, Permission.READ);
    profile = new ProfileDAO(db).findByAccountId(session.sub());
    if (profile == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Profilo non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log(null).out(profile).send();
  }

  /** PUT /api/user/users/sid — crea o aggiorna il profilo in sessione. Richiede user+. */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String nome;
    String cognome;
    String nickname;
    String immagine;
    ProfileDAO dao;
    boolean exists;

    session.require(Role.USER, Permission.WRITE);
    body     = req.body();
    nome     = (String) body.get("nome");
    cognome  = (String) body.get("cognome");
    nickname = (String) body.get("nickname");
    immagine = (String) body.get("immagine");
    Validator.required(nome,    "nome");
    Validator.required(cognome, "cognome");
    dao    = new ProfileDAO(db);
    exists = dao.existsByAccountId(session.sub());
    if (exists) {
      HashMap<String, Object> current   = dao.findByAccountId(session.sub());
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
      dao.create(session.sub(), nome, cognome, nickname, immagine, 0);
    }
    res.status(200).contentType("application/json")
       .err(false).log("Profilo aggiornato").out(null).send();
  }

  /** GET /api/user/users/sid/settings — tutte le impostazioni. Richiede user+. */
  public void settings(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    ProfileSettingsHelper helper;
    List<HashMap<String, Object>> list;

    session.require(Role.USER, Permission.READ);
    helper = new ProfileSettingsHelper(db);
    list   = helper.getAll(session.sub());
    res.status(200).contentType("application/json")
       .err(false).log(null).out(list != null ? list : Collections.emptyList()).send();
  }

  /** POST /api/user/users/sid/settings — crea o aggiorna impostazione. Richiede user+. */
  public void addSetting(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String chiave;
    String valore;
    ProfileSettingsHelper helper;

    session.require(Role.USER, Permission.WRITE);
    body   = req.body();
    chiave = (String) body.get("chiave");
    valore = (String) body.get("valore");
    Validator.required(chiave, "chiave");
    Validator.required(valore, "valore");
    helper = new ProfileSettingsHelper(db);
    if (!helper.upsert(session.sub(), chiave, valore)) {
      res.status(200).contentType("application/json")
         .err(true).log("Profilo non trovato").out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log("Impostazione salvata").out(null).send();
  }

  /** GET /api/user/users/sid/settings/{key} — singola impostazione. Richiede user+. */
  public void settingByKey(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String key;
    ProfileSettingsHelper helper;
    List<HashMap<String, Object>> all;
    HashMap<String, Object> found;

    session.require(Role.USER, Permission.READ);
    key    = req.urlArgs().get("key");
    helper = new ProfileSettingsHelper(db);
    all    = helper.getAll(session.sub());
    found  = null;
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

  /** DELETE /api/user/users/sid/settings/{key} — elimina impostazione. Richiede user+. */
  public void deleteSetting(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String key;

    session.require(Role.USER, Permission.WRITE);
    key = req.urlArgs().get("key");
    new ProfileSettingsHelper(db).delete(session.sub(), key);
    res.status(200).contentType("application/json")
       .err(false).log("Impostazione eliminata").out(null).send();
  }
}
