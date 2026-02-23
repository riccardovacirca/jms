package dev.crm.module.operatori.dao;

import dev.crm.module.operatori.dto.OperatoreCampagnaDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class OperatoreCampagnaDao
{

  private final DataSource dataSource;

  public OperatoreCampagnaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(Long operatoreId, Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO operatori_campagne (operatore_id, campagna_id, created_at) VALUES (?, ?, ?)";
      db.query(sql, operatoreId, campagnaId, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int delete(Long operatoreId, Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM operatori_campagne WHERE operatore_id = ? AND campagna_id = ?";
      return db.query(sql, operatoreId, campagnaId);
    } finally {
      db.release();
    }
  }

  public List<OperatoreCampagnaDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<OperatoreCampagnaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_campagne WHERE operatore_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<OperatoreCampagnaDto> findByCampagnaId(Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<OperatoreCampagnaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori_campagne WHERE campagna_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, campagnaId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private OperatoreCampagnaDto mapRecord(HashMap<String, Object> r)
  {
    return new OperatoreCampagnaDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toLong(r.get("campagna_id")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
