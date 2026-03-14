package {{APP_PACKAGE}}.user.dao;

import dev.jms.util.DB;

import java.util.HashMap;
import java.util.List;

/** DAO per le impostazioni utente (chiave/valore). */
public class UserSettingsDAO
{
  private final DB db;

  /** Costruttore. */
  public UserSettingsDAO(DB db)
  {
    this.db = db;
  }

  /** Restituisce tutte le impostazioni di un utente ordinate per chiave. */
  public List<HashMap<String, Object>> findAllByUserId(long userId) throws Exception
  {
    String sql;

    sql = "SELECT chiave, valore FROM user_settings WHERE user_id = ? ORDER BY chiave";
    return db.select(sql, userId);
  }

  /** Restituisce una singola impostazione. Null se non trovata. */
  public HashMap<String, Object> findByKey(long userId, String chiave) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  = "SELECT chiave, valore FROM user_settings WHERE user_id = ? AND chiave = ?";
    rows = db.select(sql, userId, chiave);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Inserisce o aggiorna un'impostazione (upsert). */
  public void upsert(long userId, String chiave, String valore) throws Exception
  {
    String sql;

    sql =
      "INSERT INTO user_settings (user_id, chiave, valore) VALUES (?, ?, ?) " +
      "ON CONFLICT (user_id, chiave) DO UPDATE SET valore = EXCLUDED.valore";
    db.query(sql, userId, chiave, valore);
  }

  /** Elimina un'impostazione. */
  public void delete(long userId, String chiave) throws Exception
  {
    String sql;

    sql = "DELETE FROM user_settings WHERE user_id = ? AND chiave = ?";
    db.query(sql, userId, chiave);
  }
}
