package dev.crm.module.sedi.dao;

import dev.crm.module.sedi.dto.SedeOperatoreDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class SedeOperatoreDao
{

  private final DataSource dataSource;

  public SedeOperatoreDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long associa(Long sedeId, Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO sedi_operatori (sede_id, operatore_id, created_at) VALUES (?, ?, ?)";
      db.query(sql, sedeId, operatoreId, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int rimuovi(Long sedeId, Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM sedi_operatori WHERE sede_id = ? AND operatore_id = ?";
      return db.query(sql, sedeId, operatoreId);
    } finally {
      db.release();
    }
  }

  public List<SedeOperatoreDto> findBySedeId(Long sedeId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<SedeOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM sedi_operatori WHERE sede_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, sedeId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<SedeOperatoreDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<SedeOperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM sedi_operatori WHERE operatore_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private SedeOperatoreDto mapRecord(HashMap<String, Object> r)
  {
    return new SedeOperatoreDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("sede_id")),
        DB.toLong(r.get("operatore_id")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
