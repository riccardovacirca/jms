package dev.crm.module.chiamate.dao;

import dev.crm.module.chiamate.dto.PromemoriaDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class PromemoriaDao
{

  private final DataSource dataSource;

  public PromemoriaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(PromemoriaDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO promemoria (richiamo_id, minuti_anticipo, trigger_at, inviato, created_at) "
          + "VALUES (?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.richiamoId,
          dto.minutiAnticipo != null ? dto.minutiAnticipo : 2,
          DB.toSqlTimestamp(dto.triggerAt),
          dto.inviato != null ? dto.inviato : 0,
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int markInviato(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE promemoria SET inviato = 1 WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  public int delete(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM promemoria WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  public int deleteByRichiamoId(Long richiamoId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM promemoria WHERE richiamo_id = ?";
      return db.query(sql, richiamoId);
    } finally {
      db.release();
    }
  }

  public List<PromemoriaDto> findByRichiamoId(Long richiamoId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<PromemoriaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM promemoria WHERE richiamo_id = ? ORDER BY trigger_at ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, richiamoId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<PromemoriaDto> findDaInviare() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<PromemoriaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM promemoria WHERE inviato = 0 AND trigger_at <= ? ORDER BY trigger_at ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, DB.toSqlTimestamp(LocalDateTime.now()));
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private PromemoriaDto mapRecord(HashMap<String, Object> r)
  {
    return new PromemoriaDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("richiamo_id")),
        DB.toInteger(r.get("minuti_anticipo")),
        DB.toLocalDateTime(r.get("trigger_at")),
        DB.toInteger(r.get("inviato")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
