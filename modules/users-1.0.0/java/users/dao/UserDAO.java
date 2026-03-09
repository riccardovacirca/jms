package {{APP_PACKAGE}}.users.dao;

import dev.jms.util.DB;

import java.util.HashMap;
import java.util.List;

/** DAO per la gestione degli account utente. */
public class UserDAO
{
  private final DB db;

  public UserDAO(DB db)
  {
    this.db = db;
  }

  /** Restituisce tutti gli account. Se search non è blank filtra per username o email. */
  public List<HashMap<String, Object>> findAll(String search) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> result;

    if (search != null && !search.isBlank()) {
      sql =
        "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
        "FROM accounts WHERE username ILIKE ? OR email ILIKE ? ORDER BY username";
      result = db.select(sql, "%" + search + "%", "%" + search + "%");
    } else {
      sql =
        "SELECT id, username, email, ruolo, attivo, must_change_password, created_at " +
        "FROM accounts ORDER BY username";
      result = db.select(sql);
    }
    return result;
  }

  /** Restituisce un account per id. Null se non trovato. */
  public HashMap<String, Object> findById(long id) throws Exception
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
  public long create(String username, String email, String passwordHash, String ruolo) throws Exception
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
  public void update(long id, String username, String email,
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

  /** Elimina un account. I refresh_tokens vengono rimossi in cascata. */
  public void delete(long id) throws Exception
  {
    String sql;

    sql = "DELETE FROM accounts WHERE id = ?";
    db.query(sql, id);
  }

  /** Conta gli account per ruolo. */
  public long countByRole(String ruolo) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  = "SELECT COUNT(*) AS cnt FROM accounts WHERE ruolo = ?";
    rows = db.select(sql, ruolo);
    return DB.toLong(rows.get(0).get("cnt"));
  }
}
