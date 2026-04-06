package dev.jms.app.crm.dao;

import dev.jms.app.crm.dto.ImportSessionDTO;
import dev.jms.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

/** DAO per la gestione delle sessioni di importazione Excel. */
public class ImportSessionDAO
{
  private final DB db;

  /** Costruisce il DAO con il database fornito. */
  public ImportSessionDAO(DB db)
  {
    this.db = db;
  }

  /** Cerca una sessione per id. Restituisce null se non trovata. */
  public ImportSessionDTO findById(String id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_import_sessions WHERE id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  /** Crea una nuova sessione di importazione con stato 'uploaded'. */
  public void create(String id, String filename, String filePath,
                     int rowCount, String headers, String preview) throws Exception
  {
    String sql;

    sql =
      "INSERT INTO jms_import_sessions (id, filename, file_path, row_count, headers, preview, status) " +
      "VALUES (?, ?, ?, ?, ?, ?, 'uploaded')";
    db.query(sql, id, filename, filePath, rowCount, headers, preview);
  }

  /** Aggiorna la mappatura colonne e imposta lo stato a 'mapped'. */
  public void updateMapping(String id, String columnMapping) throws Exception
  {
    String sql;

    sql = "UPDATE jms_import_sessions SET column_mapping = ?, status = 'mapped', updated_at = NOW() WHERE id = ?";
    db.query(sql, columnMapping, id);
  }

  /** Aggiorna lo stato e il messaggio di errore della sessione. */
  public void updateStatus(String id, String status, String errorMessage) throws Exception
  {
    String sql;

    sql = "UPDATE jms_import_sessions SET status = ?, error_message = ?, updated_at = NOW() WHERE id = ?";
    db.query(sql, status, errorMessage, id);
  }

  /** Imposta la sessione come completata. */
  public void markCompleted(String id) throws Exception
  {
    String sql;

    sql = "UPDATE jms_import_sessions SET status = 'completed', completed_at = NOW(), updated_at = NOW() WHERE id = ?";
    db.query(sql, id);
  }

  private ImportSessionDTO toDTO(HashMap<String, Object> row)
  {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime completedAt;

    createdAt   = DB.toLocalDateTime(row.get("created_at"));
    updatedAt   = DB.toLocalDateTime(row.get("updated_at"));
    completedAt = DB.toLocalDateTime(row.get("completed_at"));

    return new ImportSessionDTO(
      DB.toString(row.get("id")),
      DB.toString(row.get("filename")),
      DB.toString(row.get("file_path")),
      DB.toInteger(row.get("row_count")),
      DB.toString(row.get("headers")),
      DB.toString(row.get("preview")),
      DB.toString(row.get("column_mapping")),
      DB.toString(row.get("status")),
      DB.toString(row.get("error_message")),
      createdAt   != null ? createdAt.toString()   : null,
      updatedAt   != null ? updatedAt.toString()   : null,
      completedAt != null ? completedAt.toString() : null
    );
  }
}
