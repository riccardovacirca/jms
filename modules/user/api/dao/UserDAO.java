package {{APP_PACKAGE}}.user.dao;

import dev.jms.util.DB;

import java.util.HashMap;
import java.util.List;

/** DAO per la gestione dei profili utente. */
public class UserDAO
{
  private final DB db;

  /** Costruttore. */
  public UserDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Restituisce tutti i profili utente.
   * Se search non è blank filtra per nome, cognome o nickname.
   */
  public List<HashMap<String, Object>> findAll(String search) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> result;
    String like;

    if (search != null && !search.isBlank()) {
      like = "%" + search + "%";
      sql =
        "SELECT u.id, u.account_id, u.nome, u.cognome, u.nickname, u.immagine, " +
        "       u.flags, u.attivo, u.created_at, a.username " +
        "FROM users u JOIN accounts a ON a.id = u.account_id " +
        "WHERE u.nome ILIKE ? OR u.cognome ILIKE ? OR u.nickname ILIKE ? " +
        "ORDER BY u.cognome NULLS LAST, u.nome NULLS LAST";
      result = db.select(sql, like, like, like);
    } else {
      sql =
        "SELECT u.id, u.account_id, u.nome, u.cognome, u.nickname, u.immagine, " +
        "       u.flags, u.attivo, u.created_at, a.username " +
        "FROM users u JOIN accounts a ON a.id = u.account_id " +
        "ORDER BY u.cognome NULLS LAST, u.nome NULLS LAST";
      result = db.select(sql);
    }
    return result;
  }

  /** Restituisce un profilo per id. Null se non trovato. */
  public HashMap<String, Object> findById(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.account_id, u.nome, u.cognome, u.nickname, u.immagine, " +
      "       u.flags, u.attivo, u.created_at, a.username " +
      "FROM users u JOIN accounts a ON a.id = u.account_id " +
      "WHERE u.id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Restituisce il profilo associato a un account. Null se non trovato. */
  public HashMap<String, Object> findByAccountId(long accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql =
      "SELECT u.id, u.account_id, u.nome, u.cognome, u.nickname, u.immagine, " +
      "       u.flags, u.attivo, u.created_at, a.username " +
      "FROM users u JOIN accounts a ON a.id = u.account_id " +
      "WHERE u.account_id = ?";
    rows = db.select(sql, accountId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /** Verifica se esiste già un profilo per l'account dato. */
  public boolean existsByAccountId(long accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql  = "SELECT id FROM users WHERE account_id = ?";
    rows = db.select(sql, accountId);
    return !rows.isEmpty();
  }

  /** Verifica duplicato nickname escludendo opzionalmente un id. */
  public boolean existsByNickname(String nickname, Long excludeId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    if (nickname == null || nickname.isBlank()) {
      return false;
    }
    if (excludeId != null) {
      sql  = "SELECT id FROM users WHERE nickname = ? AND id != ?";
      rows = db.select(sql, nickname, excludeId);
    } else {
      sql  = "SELECT id FROM users WHERE nickname = ?";
      rows = db.select(sql, nickname);
    }
    return !rows.isEmpty();
  }

  /** Crea un nuovo profilo utente. Restituisce l'id generato. */
  public long create(long accountId, String nome, String cognome,
                     String nickname, String immagine, int flags) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    String nicknameVal;
    String immagineVal;

    nicknameVal = (nickname == null || nickname.isBlank()) ? null : nickname;
    immagineVal = (immagine == null || immagine.isBlank()) ? null : immagine;
    sql =
      "INSERT INTO users (account_id, nome, cognome, nickname, immagine, flags) " +
      "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
    rows = db.select(sql, accountId, nome, cognome, nicknameVal, immagineVal, flags);
    return DB.toLong(rows.get(0).get("id"));
  }

  /** Aggiorna un profilo utente. */
  public void update(long id, String nome, String cognome,
                     String nickname, String immagine, int flags, boolean attivo) throws Exception
  {
    String sql;
    String nicknameVal;
    String immagineVal;

    nicknameVal = (nickname == null || nickname.isBlank()) ? null : nickname;
    immagineVal = (immagine == null || immagine.isBlank()) ? null : immagine;
    sql =
      "UPDATE users SET nome = ?, cognome = ?, nickname = ?, immagine = ?, " +
      "                 flags = ?, attivo = ? WHERE id = ?";
    db.query(sql, nome, cognome, nicknameVal, immagineVal, flags, attivo, id);
  }

  /** Soft delete: imposta attivo = false. */
  public void softDelete(long id) throws Exception
  {
    String sql;

    sql = "UPDATE users SET attivo = false WHERE id = ?";
    db.query(sql, id);
  }
}
