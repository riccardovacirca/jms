package dev.jms.app.user.helper;

import dev.jms.app.user.dao.ProfileDAO;
import dev.jms.app.user.dao.ProfileSettingsDAO;
import dev.jms.util.DB;

import java.util.HashMap;
import java.util.List;

/**
 * Helper per le operazioni CRUD sulle impostazioni chiave/valore del profilo utente.
 */
public class ProfileSettingsHelper
{
  private final DB db;

  /** Costruttore. */
  public ProfileSettingsHelper(DB db)
  {
    this.db = db;
  }

  /**
   * Restituisce tutte le impostazioni del profilo associato all'account. Null se il profilo non esiste.
   *
   * @param accountId id dell'account autenticato
   */
  public List<HashMap<String, Object>> getAll(long accountId) throws Exception
  {
    HashMap<String, Object> profile;
    long userId;

    profile = new ProfileDAO(db).findByAccountId(accountId);
    if (profile == null) {
      return null;
    }
    userId = DB.toLong(profile.get("id"));
    return new ProfileSettingsDAO(db).findAllByUserId(userId);
  }

  /**
   * Inserisce o aggiorna un'impostazione. Restituisce false se il profilo non esiste.
   *
   * @param accountId id dell'account autenticato
   * @param chiave    chiave dell'impostazione
   * @param valore    valore da salvare
   */
  public boolean upsert(long accountId, String chiave, String valore) throws Exception
  {
    HashMap<String, Object> profile;
    long userId;

    profile = new ProfileDAO(db).findByAccountId(accountId);
    if (profile == null) {
      return false;
    }
    userId = DB.toLong(profile.get("id"));
    new ProfileSettingsDAO(db).upsert(userId, chiave, valore);
    return true;
  }

  /**
   * Elimina un'impostazione. Restituisce false se il profilo non esiste.
   *
   * @param accountId id dell'account autenticato
   * @param chiave    chiave da eliminare
   */
  public boolean delete(long accountId, String chiave) throws Exception
  {
    HashMap<String, Object> profile;
    long userId;

    profile = new ProfileDAO(db).findByAccountId(accountId);
    if (profile == null) {
      return false;
    }
    userId = DB.toLong(profile.get("id"));
    new ProfileSettingsDAO(db).delete(userId, chiave);
    return true;
  }
}
