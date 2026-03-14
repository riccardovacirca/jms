package {{APP_PACKAGE}}.auth.dao;

import {{APP_PACKAGE}}.auth.dto.AccountAuthDTO;
import {{APP_PACKAGE}}.auth.dto.AuthenticatedAccountDTO;
import dev.jms.util.DB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/** DAO per la gestione degli account e delle credenziali. */
public class AccountDAO
{
  private final DB db;

  public AccountDAO(DB db)
  {
    this.db = db;
  }

  /** Cerca account per username includendo passwordHash ed email.
   *  Usato nel flusso di login per verifica credenziali. Restituisce null se non trovato. */
  public AccountAuthDTO findForLogin(String username) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.password_hash, a.must_change_password, a.two_factor_enabled, a.email, a.ruolo, " +
      "(SELECT string_agg(permission_name, ',' ORDER BY permission_name) FROM role_permissions WHERE role_name = a.ruolo) AS permissions " +
      "FROM accounts a " +
      "WHERE a.username = ? AND a.attivo = true";
    rows = db.select(sql, username);

    return rows.isEmpty() ? null : toAccountAuthDTO(rows.get(0));
  }

  /** Cerca account per id includendo passwordHash.
   *  Usato nel flusso di cambio password per verifica password corrente. Restituisce null se non trovato. */
  public AccountAuthDTO findForPasswordChange(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.password_hash, a.must_change_password, a.two_factor_enabled, a.email, a.ruolo, " +
      "(SELECT string_agg(permission_name, ',' ORDER BY permission_name) FROM role_permissions WHERE role_name = a.ruolo) AS permissions " +
      "FROM accounts a " +
      "WHERE a.id = ? AND a.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toAccountAuthDTO(rows.get(0));
  }

  /** Cerca account per id senza passwordHash.
   *  Usato dopo verifica PIN 2FA per costruire la response. Restituisce null se non trovato. */
  public AuthenticatedAccountDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.must_change_password, a.ruolo, " +
      "(SELECT string_agg(permission_name, ',' ORDER BY permission_name) FROM role_permissions WHERE role_name = a.ruolo) AS permissions " +
      "FROM accounts a " +
      "WHERE a.id = ? AND a.attivo = true";
    rows = db.select(sql, id);

    return rows.isEmpty() ? null : toAuthenticatedAccountDTO(rows.get(0));
  }

  /** Cerca account tramite refresh token valido.
   *  Usato nel flusso di refresh per rinnovare i token. Restituisce null se non trovato o scaduto. */
  public AuthenticatedAccountDTO findByRefreshToken(String token) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT a.id, a.username, a.must_change_password, a.ruolo, " +
      "(SELECT string_agg(permission_name, ',' ORDER BY permission_name) FROM role_permissions WHERE role_name = a.ruolo) AS permissions " +
      "FROM refresh_tokens rt " +
      "JOIN accounts a ON a.id = rt.account_id " +
      "WHERE rt.token = ? AND rt.expires_at > NOW() AND a.attivo = true";
    rows = db.select(sql, token);

    return rows.isEmpty() ? null : toAuthenticatedAccountDTO(rows.get(0));
  }

  /** Aggiorna l'hash della password e il flag must_change_password. */
  public void updatePassword(int id, String passwordHash, boolean mustChangePassword) throws Exception
  {
    String sql;

    sql = "UPDATE accounts SET password_hash = ?, must_change_password = ? WHERE id = ?";
    db.query(sql, passwordHash, mustChangePassword, id);
  }

  // -------------------------
  // management CRUD
  // -------------------------

  /** Restituisce tutti gli account. Se search non è blank filtra per username o email. */
  public List<HashMap<String, Object>> findAllManagement(String search) throws Exception
  {
    String sql;

    if (search != null && !search.isBlank()) {
      sql =
        "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
        "FROM accounts WHERE username ILIKE ? OR email ILIKE ? ORDER BY username";
      return db.select(sql, "%" + search + "%", "%" + search + "%");
    }
    sql =
      "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
      "FROM accounts ORDER BY username";
    return db.select(sql);
  }

  /** Restituisce un account per id con i campi di management. Null se non trovato. */
  public HashMap<String, Object> findByIdManagement(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql =
      "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
      "FROM accounts WHERE id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Verifica duplicato username escludendo un id. */
  public boolean existsByUsername(String username, Long excludeId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    if (excludeId != null) {
      sql  = "SELECT id FROM accounts WHERE username = ? AND id != ?";
      rows = db.select(sql, username, excludeId);
    } else {
      sql  = "SELECT id FROM accounts WHERE username = ?";
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
      sql  = "SELECT id FROM accounts WHERE email = ? AND id != ?";
      rows = db.select(sql, email, excludeId);
    } else {
      sql  = "SELECT id FROM accounts WHERE email = ?";
      rows = db.select(sql, email);
    }
    return !rows.isEmpty();
  }

  /** Crea un nuovo account. Restituisce l'id generato. */
  public long createAccount(String username, String email, String passwordHash, String ruolo) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    sql  = "INSERT INTO accounts (username, email, password_hash, ruolo, must_change_password) " +
           "VALUES (?, ?, ?, ?, true) RETURNING id";
    rows = db.select(sql, username, emailVal, passwordHash, ruolo);
    return DB.toLong(rows.get(0).get("id"));
  }

  /** Aggiornamento completo (admin): include ruolo e attivo. */
  public void updateAccount(long id, String username, String email,
                            String passwordHash, String ruolo, boolean attivo) throws Exception
  {
    String sql;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    if (passwordHash != null) {
      sql = "UPDATE accounts SET username=?, email=?, password_hash=?, ruolo=?, attivo=? WHERE id=?";
      db.query(sql, username, emailVal, passwordHash, ruolo, attivo, id);
    } else {
      sql = "UPDATE accounts SET username=?, email=?, ruolo=?, attivo=? WHERE id=?";
      db.query(sql, username, emailVal, ruolo, attivo, id);
    }
  }

  /** Aggiornamento limitato (operatore): solo username, email, password. */
  public void updateSelf(long id, String username, String email, String passwordHash) throws Exception
  {
    String sql;
    String emailVal;

    emailVal = (email == null || email.isBlank()) ? null : email;
    if (passwordHash != null) {
      sql = "UPDATE accounts SET username=?, email=?, password_hash=? WHERE id=?";
      db.query(sql, username, emailVal, passwordHash, id);
    } else {
      sql = "UPDATE accounts SET username=?, email=? WHERE id=?";
      db.query(sql, username, emailVal, id);
    }
  }

  /** Soft delete: imposta attivo = false. */
  public void softDelete(long id) throws Exception
  {
    String sql;

    sql = "UPDATE accounts SET attivo = false WHERE id = ?";
    db.query(sql, id);
  }

  /** Conta gli account attivi per ruolo. */
  public long countActiveByRole(String ruolo) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  = "SELECT COUNT(*) AS cnt FROM accounts WHERE ruolo = ? AND attivo = true";
    rows = db.select(sql, ruolo);
    return DB.toLong(rows.get(0).get("cnt"));
  }

  // -------------------------
  // mapping privato
  // -------------------------

  private List<String> parsePermissions(Object raw)
  {
    String permsStr;
    permsStr = DB.toString(raw);
    if (permsStr == null || permsStr.isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.asList(permsStr.split(","));
  }

  private AccountAuthDTO toAccountAuthDTO(HashMap<String, Object> row)
  {
    return new AccountAuthDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("username")),
      DB.toString(row.get("password_hash")),
      DB.toString(row.get("ruolo")),
      parsePermissions(row.get("permissions")),
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
      parsePermissions(row.get("permissions")),
      Boolean.TRUE.equals(row.get("must_change_password"))
    );
  }
}
