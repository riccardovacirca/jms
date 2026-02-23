package dev.crm.module.auth.dao;

import dev.crm.module.auth.dto.RefreshTokenDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class RefreshTokenDao
{
  private final DataSource dataSource;

  public RefreshTokenDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public RefreshTokenDto create(Long utenteId, int expiryDays) throws Exception
  {
    DB db;
    String token;
    LocalDateTime expiresAt;
    long id;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      token = UUID.randomUUID().toString();
      expiresAt = LocalDateTime.now().plusDays(expiryDays);

      db.query(
        "INSERT INTO refresh_tokens (token, utente_id, expires_at, revoked) VALUES (?, ?, ?, ?)",
        token, utenteId, expiresAt, false
      );

      id = db.lastInsertId();
      rs = db.select("SELECT * FROM refresh_tokens WHERE id = ?", id);
      return mapToDto(rs.get(0));
    } finally {
      db.release();
    }
  }

  public Optional<RefreshTokenDto> findByToken(String token) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT * FROM refresh_tokens WHERE token = ?", token);
      if (rs.size() == 0) return Optional.empty();
      return Optional.of(mapToDto(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public void revoke(String token) throws Exception
  {
    DB db;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.query("UPDATE refresh_tokens SET revoked = ? WHERE token = ?", true, token);
    } finally {
      db.release();
    }
  }

  private RefreshTokenDto mapToDto(HashMap<String, Object> r)
  {
    RefreshTokenDto dto;

    dto = new RefreshTokenDto();
    dto.id = DB.toLong(r.get("id"));
    dto.token = DB.toString(r.get("token"));
    dto.utenteId = DB.toLong(r.get("utente_id"));
    dto.expiresAt = DB.toLocalDateTime(r.get("expires_at"));
    dto.revoked = DB.toBoolean(r.get("revoked"));
    return dto;
  }
}
