package com.example.auth.dao;

import dev.jms.util.DB;

import java.time.LocalDateTime;

public class RefreshTokenDAO
{
  private final DB db;

  public RefreshTokenDAO(DB db)
  {
    this.db = db;
  }

  /** Inserisce un nuovo refresh token. */
  public void insert(String token, int userId, LocalDateTime expiresAt) throws Exception
  {
    String sql;

    sql = "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)";
    db.query(sql, token, userId, DB.toSqlTimestamp(expiresAt));
  }

  /** Elimina un refresh token (logout o rotazione). */
  public void delete(String token) throws Exception
  {
    String sql;

    sql = "DELETE FROM refresh_tokens WHERE token = ?";
    db.query(sql, token);
  }
}
