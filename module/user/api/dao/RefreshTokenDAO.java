package dev.jms.app.user.dao;

import dev.jms.util.DB;

import java.time.LocalDateTime;

/** DAO per la gestione dei refresh token. */
public class RefreshTokenDAO
{
  private final DB db;

  /** Costruttore. */
  public RefreshTokenDAO(DB db)
  {
    this.db = db;
  }

  /** Inserisce un nuovo refresh token. */
  public void insert(String token, int accountId, LocalDateTime expiresAt) throws Exception
  {
    String sql;

    sql = "INSERT INTO jms_refresh_tokens (token, account_id, expires_at) VALUES (?, ?, ?)";
    db.query(sql, token, accountId, DB.toSqlTimestamp(expiresAt));
  }

  /** Elimina un refresh token (logout o rotazione). */
  public void delete(String token) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_refresh_tokens WHERE token = ?";
    db.query(sql, token);
  }
}
