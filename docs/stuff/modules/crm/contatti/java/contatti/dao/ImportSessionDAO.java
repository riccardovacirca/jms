package {{APP_PACKAGE}}.contatti.dao;

import {{APP_PACKAGE}}.contatti.dto.ImportSessionDTO;
import dev.jms.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class ImportSessionDAO
{
  private final DB db;

  public ImportSessionDAO(DB db)
  {
    this.db = db;
  }

  public ImportSessionDTO findById(String id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql  = "SELECT * FROM import_sessions WHERE id = ?";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  public void create(String id, String filename, String filePath,
                     int rowCount, String headers, String preview) throws Exception
  {
    String sql;

    sql =
      "INSERT INTO import_sessions (id, filename, file_path, row_count, headers, preview, status) " +
      "VALUES (?, ?, ?, ?, ?, ?, 'uploaded')";
    db.query(sql, id, filename, filePath, rowCount, headers, preview);
  }

  public void updateMapping(String id, String columnMapping) throws Exception
  {
    db.query(
      "UPDATE import_sessions SET column_mapping = ?, status = 'mapped', updated_at = NOW() WHERE id = ?",
      columnMapping, id
    );
  }

  public void updateStatus(String id, String status, String errorMessage) throws Exception
  {
    db.query(
      "UPDATE import_sessions SET status = ?, error_message = ?, updated_at = NOW() WHERE id = ?",
      status, errorMessage, id
    );
  }

  public void markCompleted(String id) throws Exception
  {
    db.query(
      "UPDATE import_sessions SET status = 'completed', completed_at = NOW(), updated_at = NOW() WHERE id = ?",
      id
    );
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
