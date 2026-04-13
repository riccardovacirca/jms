package dev.jms.app.user.dao;

import dev.jms.app.user.dto.AccountAuthDTO;
import dev.jms.app.user.dto.AuthenticatedAccountDTO;
import dev.jms.util.DB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** DAO per la gestione degli account e delle credenziali. */
public class AccountDAO
{
  private final DB db;

  /** Costruttore. */
  public AccountDAO(DB db)
  {
    this.db = db;
  }

  /** Cerca account per username includendo passwordHash ed email.
   *  Usato nel flusso di login per verifica credenziali. Null se non trovato. */
  public AccountAuthDTO findForLogin(String username) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.password_hash, a.must_change_password, a.two_factor_enabled, a.email, a.ruolo, r.level AS ruolo_level " +
      "FROM jms_user_accounts a " +
      "JOIN jms_user_roles r ON r.name = a.ruolo " +
      "WHERE a.username = ? AND a.attivo = true";
    rows = db.select(sql, username);

    return rows.isEmpty() ? null : toAccountAuthDTO(rows.get(0));
  }

  /** Cerca account per id includendo passwordHash.
   *  Usato nel flusso di cambio password. Null se non trovato. */
  public AccountAuthDTO findForPasswordChange(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.password_hash, a.must_change_password, a.two_factor_enabled, a.email, a.ruolo, r.level AS ruolo_level " +
      "FROM jms_user_accounts a " +
      "JOIN jms_user_roles r ON r.name = a.ruolo " +
      "WHERE a.id = ? AND a.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toAccountAuthDTO(rows.get(0));
  }

  /** Cerca account per id senza passwordHash. Usato dopo verifica PIN 2FA. Null se non trovato. */
  public AuthenticatedAccountDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.must_change_password, a.ruolo, r.level AS ruolo_level " +
      "FROM jms_user_accounts a " +
      "JOIN jms_user_roles r ON r.name = a.ruolo " +
      "WHERE a.id = ? AND a.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toAuthenticatedAccountDTO(rows.get(0));
  }

  /** Cerca account tramite refresh token valido. Null se non trovato o scaduto. */
  public AuthenticatedAccountDTO findByRefreshToken(String token) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.must_change_password, a.ruolo, r.level AS ruolo_level " +
      "FROM jms_user_refresh_tokens rt " +
      "JOIN jms_user_accounts a ON a.id = rt.account_id " +
      "JOIN jms_user_roles r ON r.name = a.ruolo " +
      "WHERE rt.token = ? AND rt.expires_at > NOW() AND a.attivo = true";
    rows = db.select(sql, token);

    return rows.isEmpty() ? null : toAuthenticatedAccountDTO(rows.get(0));
  }

  /** Restituisce i dati dell'account autenticato. Null se non trovato. */
  public HashMap<String, Object> findSelf(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  = "SELECT id, username, email, ruolo, attivo, must_change_password, created_at FROM jms_user_accounts WHERE id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Restituisce account per id (management). Null se non trovato. */
  public HashMap<String, Object> findByIdManagement(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  =
      "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
      "FROM jms_user_accounts WHERE id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Restituisce tutti gli account (management), con filtro opzionale. */
  public List<HashMap<String, Object>> findAll(String search, int offset, int limit) throws Exception
  {
    String sql;

    if (search != null && !search.isBlank()) {
      sql =
        "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
        "FROM jms_user_accounts WHERE username ILIKE ? OR email ILIKE ? " +
        "ORDER BY username LIMIT ? OFFSET ?";
      return db.select(sql, "%" + search + "%", "%" + search + "%", limit, offset);
    }
    sql =
      "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
      "FROM jms_user_accounts ORDER BY username LIMIT ? OFFSET ?";
    return db.select(sql, limit, offset);
  }

  /** Conta gli account, con filtro opzionale. */
  public long count(String search) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    if (search != null && !search.isBlank()) {
      sql  = "SELECT COUNT(*) AS cnt FROM jms_user_accounts WHERE username ILIKE ? OR email ILIKE ?";
      rows = db.select(sql, "%" + search + "%", "%" + search + "%");
    } else {
      sql  = "SELECT COUNT(*) AS cnt FROM jms_user_accounts";
      rows = db.select(sql);
    }
    return DB.toLong(rows.get(0).get("cnt"));
  }

  /** Verifica duplicato username escludendo un id. */
  public boolean existsByUsername(String username, Long excludeId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    if (excludeId != null) {
      sql  = "SELECT id FROM jms_user_accounts WHERE username = ? AND id != ?";
      rows = db.select(sql, username, excludeId);
    } else {
      sql  = "SELECT id FROM jms_user_accounts WHERE username = ?";
      rows = db.select(sql, username);
    }
    return !rows.isEmpty();
  }

  /** Verifica duplicato email escludendo un id. */
  public boolean existsByEmail(String email, Long excludeId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    if (email == null || email.isBlank()) {
      return false;
    }
    if (excludeId != null) {
      sql  = "SELECT id FROM jms_user_accounts WHERE email = ? AND id != ?";
      rows = db.select(sql, email, excludeId);
    } else {
      sql  = "SELECT id FROM jms_user_accounts WHERE email = ?";
      rows = db.select(sql, email);
    }
    return !rows.isEmpty();
  }

  /** Crea un nuovo account. Restituisce l'id generato. */
  public long create(String username, String email, String passwordHash, String ruolo) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    sql      =
      "INSERT INTO jms_user_accounts (username, email, password_hash, ruolo, must_change_password) " +
      "VALUES (?, ?, ?, ?, false) RETURNING id";
    rows = db.select(sql, username, emailVal, passwordHash, ruolo);
    return DB.toLong(rows.get(0).get("id"));
  }

  /** Aggiornamento admin: username, email, ruolo, attivo, must_change_password. */
  public void adminUpdate(long id, String username, String email, String ruolo, boolean attivo, boolean mustChangePassword) throws Exception
  {
    String sql;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    sql      = "UPDATE jms_user_accounts SET username=?, email=?, ruolo=?, attivo=?, must_change_password=? WHERE id=?";
    db.query(sql, username, emailVal, ruolo, attivo, mustChangePassword, id);
  }

  /** Aggiornamento self: solo username, email, password. */
  public void updateSelf(long id, String username, String email, String passwordHash) throws Exception
  {
    String sql;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    if (passwordHash != null) {
      sql = "UPDATE jms_user_accounts SET username=?, email=?, password_hash=? WHERE id=?";
      db.query(sql, username, emailVal, passwordHash, id);
    } else {
      sql = "UPDATE jms_user_accounts SET username=?, email=? WHERE id=?";
      db.query(sql, username, emailVal, id);
    }
  }

  /** Aggiorna l'hash della password e il flag must_change_password. */
  public void updatePassword(int id, String passwordHash, boolean mustChangePassword) throws Exception
  {
    String sql;

    sql = "UPDATE jms_user_accounts SET password_hash = ?, must_change_password = ? WHERE id = ?";
    db.query(sql, passwordHash, mustChangePassword, id);
  }

  /** Soft delete: imposta attivo = false. */
  public void softDelete(long id) throws Exception
  {
    String sql;

    sql = "UPDATE jms_user_accounts SET attivo = false WHERE id = ?";
    db.query(sql, id);
  }

  // ── mapping privato ──────────────────────────────────────────────────

  private AccountAuthDTO toAccountAuthDTO(HashMap<String, Object> row)
  {
    return new AccountAuthDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("username")),
      DB.toString(row.get("password_hash")),
      DB.toString(row.get("ruolo")),
      DB.toInteger(row.get("ruolo_level")),
      Boolean.TRUE.equals(row.get("must_change_password")),
      Boolean.TRUE.equals(row.get("two_factor_enabled")),
      DB.toString(row.get("email"))
    );
  }

  private AuthenticatedAccountDTO toAuthenticatedAccountDTO(HashMap<String, Object> row)
  {
    return new AuthenticatedAccountDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("username")),
      DB.toString(row.get("ruolo")),
      DB.toInteger(row.get("ruolo_level")),
      Boolean.TRUE.equals(row.get("must_change_password"))
    );
  }
}
