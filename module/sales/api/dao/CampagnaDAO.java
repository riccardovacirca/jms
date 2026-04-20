package dev.jms.app.sales.dao;

import dev.jms.app.sales.dto.CampagnaDTO;
import dev.jms.app.sales.dto.ListaDTO;
import dev.jms.util.DB;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** DAO per la gestione delle campagne e delle loro liste associate. */
public class CampagnaDAO
{
  private final DB db;

  public CampagnaDAO(DB db)
  {
    this.db = db;
  }

  /** Restituisce le campagne non cancellate, paginate. */
  public List<CampagnaDTO> findAll(int page, int size) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT c.*, COUNT(cl.lista_id) AS liste_count " +
      "FROM jms_sales_campagne c " +
      "LEFT JOIN jms_sales_campagna_liste cl ON cl.campagna_id = c.id " +
      "WHERE c.deleted_at IS NULL " +
      "GROUP BY c.id " +
      "ORDER BY c.nome " +
      "LIMIT ? OFFSET ?";
    rows = db.select(sql, size, (page - 1) * size);
    return rows.stream().map(this::toDTO).toList();
  }

  /** Conta le campagne non cancellate per la paginazione. */
  public int count() throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_sales_campagne WHERE deleted_at IS NULL";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Cerca per id. Restituisce null se non trovata o soft-deleted. */
  public CampagnaDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT c.*, COUNT(cl.lista_id) AS liste_count " +
      "FROM jms_sales_campagne c " +
      "LEFT JOIN jms_sales_campagna_liste cl ON cl.campagna_id = c.id " +
      "WHERE c.id = ? AND c.deleted_at IS NULL " +
      "GROUP BY c.id";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  /** Inserisce una nuova campagna. Restituisce l'id generato. */
  public int insert(CampagnaDTO c) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "INSERT INTO jms_sales_campagne (nome, descrizione, stato) " +
      "VALUES (?, ?, ?) " +
      "RETURNING id";
    rows = db.select(sql, c.nome(), c.descrizione(), c.stato());
    return DB.toInteger(rows.get(0).get("id"));
  }

  /** Aggiorna tutti i campi modificabili della campagna. */
  public void update(CampagnaDTO c) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sales_campagne SET nome = ?, descrizione = ?, stato = ?, updated_at = NOW() WHERE id = ?";
    db.query(sql, c.nome(), c.descrizione(), c.stato(), c.id());
  }

  /** Soft delete: imposta deleted_at. */
  public void delete(int id) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sales_campagne SET deleted_at = NOW() WHERE id = ?";
    db.query(sql, id);
  }

  /** Verifica se esiste già una campagna con il nome, escludendo opzionalmente un id. */
  public boolean existsByNome(String nome, Integer excludeId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    if (excludeId != null) {
      sql = "SELECT COUNT(*) AS n FROM jms_sales_campagne WHERE nome = ? AND id != ? AND deleted_at IS NULL";
      rows = db.select(sql, nome, excludeId);
    } else {
      sql = "SELECT COUNT(*) AS n FROM jms_sales_campagne WHERE nome = ? AND deleted_at IS NULL";
      rows = db.select(sql, nome);
    }
    return DB.toInteger(rows.get(0).get("n")) > 0;
  }

  // -------------------------
  // liste della campagna
  // -------------------------

  /** Restituisce le liste associate alla campagna, paginate. */
  public List<ListaDTO> findListe(int campagnaId, int page, int size) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT l.*, COUNT(lc.contatto_id) AS contatti_count " +
      "FROM jms_sales_campagna_liste cl " +
      "JOIN jms_sales_liste l ON l.id = cl.lista_id " +
      "LEFT JOIN jms_sales_lista_contatti lc ON lc.lista_id = l.id " +
      "WHERE cl.campagna_id = ? AND l.deleted_at IS NULL " +
      "GROUP BY l.id " +
      "ORDER BY l.nome " +
      "LIMIT ? OFFSET ?";
    rows = db.select(sql, campagnaId, size, (page - 1) * size);
    return rows.stream().map(this::toListaDTO).toList();
  }

  /** Conta le liste associate alla campagna per la paginazione. */
  public int countListe(int campagnaId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT COUNT(*) AS n " +
      "FROM jms_sales_campagna_liste cl " +
      "JOIN jms_sales_liste l ON l.id = cl.lista_id " +
      "WHERE cl.campagna_id = ? AND l.deleted_at IS NULL";
    rows = db.select(sql, campagnaId);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Aggiunge una lista alla campagna. Ignora se già presente (ON CONFLICT DO NOTHING). */
  public void addLista(int campagnaId, int listaId) throws Exception
  {
    String sql;

    sql = "INSERT INTO jms_sales_campagna_liste (campagna_id, lista_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
    db.query(sql, campagnaId, listaId);
  }

  /** Rimuove una lista dalla campagna. */
  public void removeLista(int campagnaId, int listaId) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_sales_campagna_liste WHERE campagna_id = ? AND lista_id = ?";
    db.query(sql, campagnaId, listaId);
  }

  // -------------------------
  // mapping privato
  // -------------------------

  private CampagnaDTO toDTO(HashMap<String, Object> row)
  {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    createdAt = DB.toLocalDateTime(row.get("created_at"));
    updatedAt = DB.toLocalDateTime(row.get("updated_at"));
    deletedAt = DB.toLocalDateTime(row.get("deleted_at"));

    return new CampagnaDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("nome")),
      DB.toString(row.get("descrizione")),
      DB.toInteger(row.get("stato")),
      createdAt != null ? createdAt.toString() : null,
      updatedAt != null ? updatedAt.toString() : null,
      deletedAt != null ? deletedAt.toString() : null,
      DB.toLong(row.get("liste_count"))
    );
  }

  private ListaDTO toListaDTO(HashMap<String, Object> row)
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
}
