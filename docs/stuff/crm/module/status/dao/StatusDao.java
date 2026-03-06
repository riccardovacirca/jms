package dev.crm.module.status.dao;

import dev.crm.module.status.dto.StatusLogDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class StatusDao
{
  private final DataSource dataSource;

  public StatusDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insertLog(String message) throws Exception
  {
    DB db;
    String sql;
    long id;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "INSERT INTO status_logs (message, created_at) VALUES (?, ?)";
      db.query(sql, message, DB.toSqlTimestamp(LocalDateTime.now()));
      id = db.lastInsertId();
      return id;
    } finally {
      db.release();
    }
  }

  public List<StatusLogDto> findLogs(int limit, int offset) throws Exception
  {
    DB db;
    List<StatusLogDto> logs;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      logs = new ArrayList<>();
      sql = "SELECT id, message, created_at " +
            "FROM status_logs ORDER BY id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        logs.add(
            new StatusLogDto(
                DB.toLong(r.get("id")),
                DB.toString(r.get("message")),
                DB.toLocalDateTime(r.get("created_at"))));
      }
      return logs;
    } finally {
      db.release();
    }
  }
}
