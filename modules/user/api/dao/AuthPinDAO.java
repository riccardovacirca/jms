package {{APP_PACKAGE}}.user.dao;

import {{APP_PACKAGE}}.user.dto.AuthPinDTO;
import dev.jms.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

/** DAO per la gestione dei PIN di autenticazione a due fattori. */
public class AuthPinDAO
{
  private final DB db;

  /** Costruttore. */
  public AuthPinDAO(DB db)
  {
    this.db = db;
  }

  /** Rimuove i PIN scaduti e quelli esistenti dell'utente prima di inserirne uno nuovo. */
  public void cleanup(int accountId) throws Exception
  {
    String sql;

    sql = "DELETE FROM auth_pins WHERE account_id = ? OR expires_at < NOW()";
    db.query(sql, accountId);
  }

  /** Inserisce un nuovo PIN. */
  public void insert(String challengeToken, int accountId, String pinHash, LocalDateTime expiresAt) throws Exception
  {
    String sql;

    sql = "INSERT INTO auth_pins (challenge_token, account_id, pin_hash, expires_at) VALUES (?, ?, ?, ?)";
    db.query(sql, challengeToken, accountId, pinHash, DB.toSqlTimestamp(expiresAt));
  }

  /** Cerca un PIN per challenge token. Null se non trovato. */
  public AuthPinDTO findByToken(String challengeToken) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> row;

    sql  = "SELECT id, account_id, pin_hash, expires_at FROM auth_pins WHERE challenge_token = ?";
    rows = db.select(sql, challengeToken);

    if (rows.isEmpty()) {
      return null;
    }
    row = rows.get(0);
    return new AuthPinDTO(
      DB.toLong(row.get("id")),
      DB.toInteger(row.get("account_id")),
      DB.toString(row.get("pin_hash")),
      DB.toLocalDateTime(row.get("expires_at"))
    );
  }

  /** Elimina un PIN per challenge token. */
  public void deleteByToken(String challengeToken) throws Exception
  {
    String sql;

    sql = "DELETE FROM auth_pins WHERE challenge_token = ?";
    db.query(sql, challengeToken);
  }
}
