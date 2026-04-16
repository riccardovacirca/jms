package dev.jms.app.crm.dao;

import dev.jms.app.crm.dto.ListaContattoDTO;
import dev.jms.app.crm.dto.ListaDTO;
import dev.jms.util.DB;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** DAO per la gestione delle liste di contatti. */
public class ListaDAO
{
  private final DB db;

  public ListaDAO(DB db)
  {
    this.db = db;
  }

  /** Restituisce le liste attive (non cancellate) paginate. */
  public List<ListaDTO> findAll(int page, int size) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT l.*, COUNT(lc.contatto_id) AS contatti_count " +
      "FROM jms_crm_liste l " +
      "LEFT JOIN jms_crm_lista_contatti lc ON lc.lista_id = l.id " +
      "WHERE l.deleted_at IS NULL " +
      "GROUP BY l.id " +
      "ORDER BY l.nome " +
      "LIMIT ? OFFSET ?";
    rows = db.select(sql, size, (page - 1) * size);
    return rows.stream().map(this::toDTO).toList();
  }

  /** Conta le liste non cancellate per la paginazione. */
  public int count() throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_crm_liste WHERE deleted_at IS NULL";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Cerca per id. Restituisce null se non trovata o soft-deleted. */
  public ListaDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT l.*, COUNT(lc.contatto_id) AS contatti_count " +
      "FROM jms_crm_liste l " +
      "LEFT JOIN jms_crm_lista_contatti lc ON lc.lista_id = l.id " +
      "WHERE l.id = ? AND l.deleted_at IS NULL " +
      "GROUP BY l.id";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  /** Inserisce una nuova lista. Restituisce l'id generato. */
  public int insert(ListaDTO l) throws Exception
  {
    String sql;
    Object scadenza;
    ArrayList<HashMap<String, Object>> rows;

    scadenza = l.scadenza() != null && !l.scadenza().isBlank() ? java.sql.Date.valueOf(l.scadenza()) : null;
    sql =
      "INSERT INTO jms_crm_liste (nome, descrizione, consenso, stato, scadenza) " +
      "VALUES (?, ?, ?, ?, ?) " +
      "RETURNING id";
    rows = db.select(sql, l.nome(), l.descrizione(), l.consenso(), l.stato(), scadenza);
    return DB.toInteger(rows.get(0).get("id"));
  }

  /** Aggiorna tutti i campi della lista. */
  public void update(ListaDTO l) throws Exception
  {
    String sql;
    Object scadenza;

    scadenza = l.scadenza() != null && !l.scadenza().isBlank() ? java.sql.Date.valueOf(l.scadenza()) : null;
    sql = "UPDATE jms_crm_liste SET nome = ?, descrizione = ?, consenso = ?, stato = ?, scadenza = ?, updated_at = NOW() WHERE id = ?";
    db.query(sql, l.nome(), l.descrizione(), l.consenso(), l.stato(), scadenza, l.id());
  }

  /** Restituisce la lista marcata come default, o null se non configurata. */
  public ListaDTO findDefault() throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT l.*, COUNT(lc.contatto_id) AS contatti_count " +
      "FROM jms_crm_liste l " +
      "LEFT JOIN jms_crm_lista_contatti lc ON lc.lista_id = l.id " +
      "WHERE l.is_default = TRUE AND l.deleted_at IS NULL " +
      "GROUP BY l.id";
    rows = db.select(sql);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  /**
   * Imposta la lista indicata come default, rimuovendo il flag da tutte le altre.
   * Operazione atomica eseguita in transazione.
   */
  public void setDefault(int id) throws Exception
  {
    String sql;

    db.begin();
    try {
      sql = "UPDATE jms_crm_liste SET is_default = FALSE WHERE is_default = TRUE";
      db.query(sql);
      sql = "UPDATE jms_crm_liste SET is_default = TRUE, updated_at = NOW() WHERE id = ? AND deleted_at IS NULL";
      db.query(sql, id);
      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    }
  }

  /** Soft delete: imposta deleted_at. */
  public void delete(int id) throws Exception
  {
    String sql;

    sql = "UPDATE jms_crm_liste SET deleted_at = NOW() WHERE id = ?";
    db.query(sql, id);
  }

  /** Aggiorna solo il campo stato. */
  public void updateStato(int id, int stato) throws Exception
  {
    String sql;

    sql = "UPDATE jms_crm_liste SET stato = ?, updated_at = NOW() WHERE id = ?";
    db.query(sql, stato, id);
  }

  /** Aggiorna solo la scadenza. */
  public void updateScadenza(int id, String scadenza) throws Exception
  {
    String sql;
    Object scadenzaVal;

    scadenzaVal = scadenza != null && !scadenza.isBlank() ? java.sql.Date.valueOf(scadenza) : null;
    sql = "UPDATE jms_crm_liste SET scadenza = ?, updated_at = NOW() WHERE id = ?";
    db.query(sql, scadenzaVal, id);
  }

  /** Verifica se esiste già una lista con il nome, escludendo opzionalmente un id. */
  public boolean existsByNome(String nome, Integer excludeId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    if (excludeId != null) {
      sql = "SELECT COUNT(*) AS n FROM jms_crm_liste WHERE nome = ? AND id != ? AND deleted_at IS NULL";
      rows = db.select(sql, nome, excludeId);
    } else {
      sql = "SELECT COUNT(*) AS n FROM jms_crm_liste WHERE nome = ? AND deleted_at IS NULL";
      rows = db.select(sql, nome);
    }
    return DB.toInteger(rows.get(0).get("n")) > 0;
  }

  // -------------------------
  // contatti della lista
  // -------------------------

  /** Restituisce i contatti di una lista, paginati. */
  public List<ListaContattoDTO> findContatti(int listaId, int page, int size) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT lc.id, lc.lista_id, lc.contatto_id, lc.created_at, " +
      "       c.nome, c.cognome, c.telefono " +
      "FROM jms_crm_lista_contatti lc " +
      "JOIN jms_crm_contatti c ON c.id = lc.contatto_id " +
      "WHERE lc.lista_id = ? " +
      "ORDER BY c.cognome, c.nome " +
      "LIMIT ? OFFSET ?";
    rows = db.select(sql, listaId, size, (page - 1) * size);
    return rows.stream().map(this::toContattoDTO).toList();
  }

  /** Conta i contatti di una lista per la paginazione. */
  public int countContatti(int listaId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_crm_lista_contatti WHERE lista_id = ?";
    rows = db.select(sql, listaId);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Aggiunge un contatto alla lista. Ignora se già presente (ON CONFLICT DO NOTHING). */
  public void addContatto(int listaId, int contattoId) throws Exception
  {
    String sql;

    sql = "INSERT INTO jms_crm_lista_contatti (lista_id, contatto_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    db.query(sql, listaId, contattoId);
  }

  /** Rimuove un contatto dalla lista. */
  public void removeContatto(int listaId, int contattoId) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_crm_lista_contatti WHERE lista_id = ? AND contatto_id = ?";
    db.query(sql, listaId, contattoId);
  }

  // -------------------------
  // mapping privato
  // -------------------------

  private ListaDTO toDTO(HashMap<String, Object> row)
  {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    LocalDate scadenza;

    createdAt = DB.toLocalDateTime(row.get("created_at"));
    updatedAt = DB.toLocalDateTime(row.get("updated_at"));
    deletedAt = DB.toLocalDateTime(row.get("deleted_at"));
    scadenza  = DB.toLocalDate(row.get("scadenza"));

    return new ListaDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("nome")),
      DB.toString(row.get("descrizione")),
      DB.toBoolean(row.get("consenso")),
      DB.toInteger(row.get("stato")),
      scadenza  != null ? scadenza.toString()  : null,
      createdAt != null ? createdAt.toString() : null,
      updatedAt != null ? updatedAt.toString() : null,
      deletedAt != null ? deletedAt.toString() : null,
      DB.toBoolean(row.get("is_default")),
      DB.toLong(row.get("contatti_count"))
    );
  }

  private ListaContattoDTO toContattoDTO(HashMap<String, Object> row)
  {
    LocalDateTime createdAt;

    createdAt = DB.toLocalDateTime(row.get("created_at"));

    return new ListaContattoDTO(
      DB.toInteger(row.get("id")),
      DB.toInteger(row.get("lista_id")),
      DB.toInteger(row.get("contatto_id")),
      createdAt != null ? createdAt.toString() : null,
      DB.toString(row.get("nome")),
      DB.toString(row.get("cognome")),
      DB.toString(row.get("telefono"))
    );
  }
}
