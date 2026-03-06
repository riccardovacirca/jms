package dev.crm.module.operatori.dao;

import dev.crm.module.operatori.dto.AttivitaOperatoreDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class AttivitaOperatoreDao
{

  private final DataSource dataSource;

  public AttivitaOperatoreDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(Long operatoreId, String azione, String descrizione) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO operatori_attivita (operatore_id, azione, descrizione, timestamp) VALUES (?, ?, ?, ?)";
      db.query(sql, operatoreId, azione, descrizione, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public List<AttivitaOperatoreDto> findByOperatoreId(Long operatoreId, Integer limit)
      throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<AttivitaOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_attivita WHERE operatore_id = ? ORDER BY timestamp DESC";
      if (limit != null && limit > 0) {
        sql += " LIMIT " + limit;
      }
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<AttivitaOperatoreDto> findAll(Integer limit) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<AttivitaOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_attivita ORDER BY timestamp DESC";
      if (limit != null && limit > 0) {
        sql += " LIMIT " + limit;
      }
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private AttivitaOperatoreDto mapRecord(HashMap<String, Object> r)
  {
    return new AttivitaOperatoreDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toString(r.get("azione")),
        DB.toString(r.get("descrizione")),
        DB.toLocalDateTime(r.get("timestamp")));
  }
}
