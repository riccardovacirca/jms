package dev.crm.module.importer.dao;

import dev.crm.module.importer.dto.ImportSessionDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class ImportSessionDao
{

  private final DataSource dataSource;

  public ImportSessionDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /** Crea una nuova sessione di importazione */
  public String
  insert(String id, String filename, String filePath,
         String headersJson, int rowCount) throws Exception
  {
    DB db;
    String sql;
    String result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "INSERT INTO import_sessions ("
          + "id, filename, file_path, headers, row_count, status, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          id,
          filename,
          filePath,
          headersJson,
          rowCount,
          "uploaded",
          DB.toSqlTimestamp(LocalDateTime.now()));
      result = id;
      return result;
    } finally {
      db.release();
    }
  }

  /** Trova una sessione per ID */
  public Optional<ImportSessionDto> findById(String id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<ImportSessionDto> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT * FROM import_sessions " + "WHERE id = ?";
      rs = db.select(sql, id);
      if (rs.isEmpty()) {
        result = Optional.empty();
        return result;
      }
      result = Optional.of(mapRecord(rs.get(0)));
      return result;
    } finally {
      db.release();
    }
  }

  /** Aggiorna il mapping delle colonne */
  public int updateMapping(String id, String columnMappingJson) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE import_sessions SET "
          + "column_mapping = ?, status = ?, updated_at = ? "
          + "WHERE id = ?";
      result = db.query(sql, columnMappingJson, "mapped",
                        DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  /** Aggiorna lo stato della sessione */
  public int updateStatus(String id, String status) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE import_sessions " + "SET " +
            "status = ?, updated_at = ? WHERE id = ?";
      result = db.query(sql, status, DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  /** Segna la sessione come completata */
  public int markCompleted(String id) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE import_sessions SET " +
            "status = ?, completed_at = ?, updated_at = ? WHERE id = ?";
      result = db.query(
          sql,
          "completed",
          DB.toSqlTimestamp(LocalDateTime.now()),
          DB.toSqlTimestamp(LocalDateTime.now()),
          id);
      return result;
    } finally {
      db.release();
    }
  }

  /** Segna la sessione come fallita */
  public int markFailed(String id, String errorMessage) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE import_sessions SET " +
            "status = ?, error_message = ?, updated_at = ? WHERE id = ?";
      result = db.query(sql, "failed", errorMessage,
                        DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  /** Trova tutte le sessioni ordinate per data */
  public List<ImportSessionDto> findAll(int limit, int offset) throws Exception
  {
    DB db;
    List<ImportSessionDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM import_sessions " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /** Conta tutte le sessioni */
  public int count() throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM import_sessions";
      rs = db.select(sql);
      if (!rs.isEmpty()) {
        result = DB.toInteger(rs.get(0).get("cnt"));
        return result;
      }
      result = 0;
      return result;
    } finally {
      db.release();
    }
  }

  /** Elimina una sessione (e il file associato andrebbe eliminato separatamente) */
  public int delete(String id) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "DELETE FROM import_sessions WHERE id = ?";
      result = db.query(sql, id);
      return result;
    } finally {
      db.release();
    }
  }

  /** Recupera il file path di una sessione */
  public String getFilePath(String id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    String result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT file_path FROM import_sessions WHERE id = ?";
      rs = db.select(sql, id);
      if (!rs.isEmpty()) {
        result = DB.toString(rs.get(0).get("file_path"));
        return result;
      }
      result = null;
      return result;
    } finally {
      db.release();
    }
  }

  /** Recupera il column mapping di una sessione */
  public String getColumnMapping(String id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    String result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT column_mapping FROM import_sessions WHERE id = ?";
      rs = db.select(sql, id);
      if (!rs.isEmpty()) {
        result = DB.toString(rs.get(0).get("column_mapping"));
        return result;
      }
      result = null;
      return result;
    } finally {
      db.release();
    }
  }

  private ImportSessionDto mapRecord(HashMap<String, Object> r)
  {
    ImportSessionDto dto;

    dto = new ImportSessionDto();
    dto.sessionId = DB.toString(r.get("id"));
    dto.filename = DB.toString(r.get("filename"));
    dto.rowCount = DB.toInteger(r.get("row_count"));
    dto.status = DB.toString(r.get("status"));
    dto.createdAt = DB.toLocalDateTime(r.get("created_at"));
    // headers e previewRows vengono popolati dal service quando necessario
    return dto;
  }
}
