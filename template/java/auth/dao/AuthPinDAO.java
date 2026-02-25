package {{APP_PACKAGE}}.auth.dao;

import {{APP_PACKAGE}}.auth.dto.AuthPinDTO;
import dev.jms.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthPinDAO
{
  private final DB db;

  public AuthPinDAO(DB db)
  {
    this.db = db;
  }

  /** Rimuove i PIN scaduti e quelli esistenti dell'utente prima di inserirne uno nuovo. */
  public void cleanup(int userId) throws Exception
  {
    String sql;

    sql = "DELETE FROM auth_pins WHERE user_id = ? OR expires_at < NOW()";
    db.query(sql, userId);
  }

  /** Inserisce un nuovo PIN. */
  public void insert(String challengeToken, int userId, String pinHash, LocalDateTime expiresAt) throws Exception
  {
    String sql;

    sql = "INSERT INTO auth_pins (challenge_token, user_id, pin_hash, expires_at) VALUES (?, ?, ?, ?)";
    db.query(sql, challengeToken, userId, pinHash, DB.toSqlTimestamp(expiresAt));
  }

  /** Cerca un PIN per challenge token. Restituisce null se non trovato. */
  public AuthPinDTO findByToken(String challengeToken) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> row;
    AuthPinDTO result;

    sql = "SELECT id, user_id, pin_hash, expires_at FROM auth_pins WHERE challenge_token = ?";
    rows = db.select(sql, challengeToken);

    if (rows.isEmpty()) {
      result = null;
    } else {
      row = rows.get(0);
      result = new AuthPinDTO(
        DB.toLong(row.get("id")),
        DB.toInteger(row.get("user_id")),
        DB.toString(row.get("pin_hash")),
        DB.toLocalDateTime(row.get("expires_at"))
      );
    }

    return result;
  }

  /** Elimina un PIN per challenge token. */
  public void deleteByToken(String challengeToken) throws Exception
  {
    String sql;

    sql = "DELETE FROM auth_pins WHERE challenge_token = ?";
    db.query(sql, challengeToken);
  }
}
