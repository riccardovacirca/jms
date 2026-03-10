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
