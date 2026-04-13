package dev.jms.app.user.dao;

import dev.jms.util.DB;

import java.util.ArrayList;
import java.util.HashMap;

/** DAO per la gestione dei token di reset password. */
public class PasswordResetDAO
{
  private final DB db;

  /** Costruttore. */
  public PasswordResetDAO(DB db)
  {
    this.db = db;
  }

  /** Salva un token di reset per l'utente con scadenza a 1 ora. */
  public void saveToken(int accountId, String token) throws Exception
  {
    String sql;

    sql = "INSERT INTO jms_user_password_reset_tokens (token, account_id, expires_at) VALUES (?, ?, NOW() + INTERVAL '1 hour')";
    db.query(sql, token, accountId);
  }

  /** Cerca un token valido (non scaduto, non usato). Restituisce l'accountId o null se non trovato. */
  public Integer findValidAccountId(String token) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql  = "SELECT account_id FROM jms_user_password_reset_tokens WHERE token = ? AND expires_at > NOW() AND used = false";
    rows = db.select(sql, token);

    return rows.isEmpty() ? null : DB.toInteger(rows.get(0).get("account_id"));
  }

  /** Marca il token come usato impedendo ulteriori reset con lo stesso link. */
  public void markUsed(String token) throws Exception
  {
    String sql;

    sql = "UPDATE jms_user_password_reset_tokens SET used = true WHERE token = ?";
    db.query(sql, token);
  }
}
