package dev.crm.module.logs.dao;

import dev.crm.module.logs.dto.LogDto;
import dev.springtools.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;

public class LogDao
{
  private final DataSource dataSource;

  public LogDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public List<LogDto> findAll(int limit, int offset) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    List<LogDto> list;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select(
          "SELECT * FROM logs ORDER BY timestamp DESC LIMIT ? OFFSET ?", limit, offset);
      list = new ArrayList<>();
      for (int i = 0; i < rs.size(); i++) {
        list.add(mapToDto(rs.get(i)));
      }
      return list;
    } finally {
      db.release();
    }
  }

  public List<LogDto> findByModule(String module, int limit, int offset) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    List<LogDto> list;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select(
          "SELECT * FROM logs WHERE module = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
          module,
          limit,
          offset);
      list = new ArrayList<>();
      for (int i = 0; i < rs.size(); i++) {
        list.add(mapToDto(rs.get(i)));
      }
      return list;
    } finally {
      db.release();
    }
  }

  public List<LogDto> findByLevel(String level, int limit, int offset) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    List<LogDto> list;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select(
          "SELECT * FROM logs WHERE level = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
          level,
          limit,
          offset);
      list = new ArrayList<>();
      for (int i = 0; i < rs.size(); i++) {
        list.add(mapToDto(rs.get(i)));
      }
      return list;
    } finally {
      db.release();
    }
  }

  public LogDto findById(Long id) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    LogDto dto;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT * FROM logs WHERE id = ?", id);
      if (rs.size() == 0) {
        return null;
      }
      dto = mapToDto(rs.get(0));
      return dto;
    } finally {
      db.release();
    }
  }

  public Long insert(LogDto dto) throws Exception
  {
    DB db;
    Long id;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      db.query(
          "INSERT INTO logs (level, module, message, data, user_id, session_id, ip_address, user_agent) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
          dto.level,
          dto.module,
          dto.message,
          dto.data,
          dto.userId,
          dto.sessionId,
          dto.ipAddress,
          dto.userAgent);

      id = db.lastInsertId();
      db.commit();
      return id;
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }

  public Integer delete(Long id) throws Exception
  {
    DB db;
    int affected;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      affected = db.query("DELETE FROM logs WHERE id = ?", id);

      db.commit();
      return affected;
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }

  public Integer deleteOlderThan(int days) throws Exception
  {
    DB db;
    int affected;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      affected = db.query(
          "DELETE FROM logs WHERE timestamp < datetime('now', '-' || ? || ' days')", days);

      db.commit();
      return affected;
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }

  private LogDto mapToDto(java.util.Map<String, Object> row)
  {
    LogDto dto;
    Object idObj;
    Object userIdObj;

    dto = new LogDto();
    idObj = row.get("id");
    dto.id = idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    dto.timestamp = (String) row.get("timestamp");
    dto.level = (String) row.get("level");
    dto.module = (String) row.get("module");
    dto.message = (String) row.get("message");
    dto.data = (String) row.get("data");
    userIdObj = row.get("user_id");
    dto.userId = userIdObj == null ? null
        : (userIdObj instanceof Integer ? ((Integer) userIdObj).longValue() : (Long) userIdObj);
    dto.sessionId = (String) row.get("session_id");
    dto.ipAddress = (String) row.get("ip_address");
    dto.userAgent = (String) row.get("user_agent");
    dto.createdAt = (String) row.get("created_at");
    return dto;
  }
}
