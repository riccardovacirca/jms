package dev.crm.module.operatori.dao;

import dev.crm.module.operatori.dto.StatoOperatoreDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatoOperatoreDao
{

  private final DataSource dataSource;

  public StatoOperatoreDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(Long operatoreId, String stato) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO operatori_stato (operatore_id, stato, timestamp) VALUES (?, ?, ?)";
      db.query(sql, operatoreId, stato, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public List<StatoOperatoreDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<StatoOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_stato WHERE operatore_id = ? ORDER BY timestamp DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<StatoOperatoreDto> findAll() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<StatoOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_stato ORDER BY timestamp DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private StatoOperatoreDto mapRecord(HashMap<String, Object> r)
  {
    return new StatoOperatoreDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toString(r.get("stato")),
        DB.toLocalDateTime(r.get("timestamp")));
  }
}
