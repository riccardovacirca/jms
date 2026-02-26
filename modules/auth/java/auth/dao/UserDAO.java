package com.example.auth.dao;

import com.example.auth.dto.AuthenticatedUserDTO;
import com.example.auth.dto.UserAuthDTO;
import dev.jms.util.DB;

import java.util.ArrayList;
import java.util.HashMap;

public class UserDAO
{
  private final DB db;

  public UserDAO(DB db)
  {
    this.db = db;
  }

  /** Cerca utente per username includendo passwordHash ed email.
   *  Usato nel flusso di login per verifica credenziali. Restituisce null se non trovato. */
  public UserAuthDTO findForLogin(String username) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.username, u.password_hash, u.must_change_password, u.two_factor_enabled, u.email, r.name AS ruolo, " +
      "r.can_admin, r.can_write, r.can_delete " +
      "FROM users u JOIN roles r ON r.name = u.ruolo " +
      "WHERE u.username = ? AND u.attivo = true";
    rows = db.select(sql, username);

    return rows.isEmpty() ? null : toUserAuthDTO(rows.get(0));
  }

  /** Cerca utente per id includendo passwordHash.
   *  Usato nel flusso di cambio password per verifica password corrente. Restituisce null se non trovato. */
  public UserAuthDTO findForPasswordChange(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.username, u.password_hash, u.must_change_password, u.two_factor_enabled, u.email, r.name AS ruolo, " +
      "r.can_admin, r.can_write, r.can_delete " +
      "FROM users u JOIN roles r ON r.name = u.ruolo " +
      "WHERE u.id = ? AND u.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toUserAuthDTO(rows.get(0));
  }

  /** Cerca utente per id senza passwordHash.
   *  Usato dopo verifica PIN 2FA per costruire la response. Restituisce null se non trovato. */
  public AuthenticatedUserDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.username, u.must_change_password, r.name AS ruolo, " +
      "r.can_admin, r.can_write, r.can_delete " +
      "FROM users u JOIN roles r ON r.name = u.ruolo " +
      "WHERE u.id = ? AND u.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toAuthenticatedUserDTO(rows.get(0));
  }

  /** Cerca utente tramite refresh token valido.
   *  Usato nel flusso di refresh per rinnovare i token. Restituisce null se non trovato o scaduto. */
  public AuthenticatedUserDTO findByRefreshToken(String token) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.username, u.must_change_password, r.name AS ruolo, " +
      "r.can_admin, r.can_write, r.can_delete " +
      "FROM refresh_tokens rt " +
      "JOIN users u ON u.id = rt.user_id " +
      "JOIN roles r ON r.name = u.ruolo " +
      "WHERE rt.token = ? AND rt.expires_at > NOW() AND u.attivo = true";
    rows = db.select(sql, token);

    return rows.isEmpty() ? null : toAuthenticatedUserDTO(rows.get(0));
  }

  /** Aggiorna l'hash della password e il flag must_change_password. */
  public void updatePassword(int id, String passwordHash, boolean mustChangePassword) throws Exception
  {
    String sql;

    sql = "UPDATE users SET password_hash = ?, must_change_password = ? WHERE id = ?";
    db.query(sql, passwordHash, mustChangePassword, id);
  }

  // -------------------------
  // mapping privato
  // -------------------------

  private UserAuthDTO toUserAuthDTO(HashMap<String, Object> row)
  {
    return new UserAuthDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("username")),
      DB.toString(row.get("password_hash")),
      DB.toString(row.get("ruolo")),
      Boolean.TRUE.equals(row.get("can_admin")),
      Boolean.TRUE.equals(row.get("can_write")),
      Boolean.TRUE.equals(row.get("can_delete")),
      Boolean.TRUE.equals(row.get("must_change_password")),
      Boolean.TRUE.equals(row.get("two_factor_enabled")),
      DB.toString(row.get("email"))
    );
  }

  private AuthenticatedUserDTO toAuthenticatedUserDTO(HashMap<String, Object> row)
  {
    return new AuthenticatedUserDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("username")),
      DB.toString(row.get("ruolo")),
      Boolean.TRUE.equals(row.get("can_admin")),
      Boolean.TRUE.equals(row.get("can_write")),
      Boolean.TRUE.equals(row.get("can_delete")),
      Boolean.TRUE.equals(row.get("must_change_password"))
    );
  }
}
