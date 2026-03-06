package dev.crm.module.sedi.dao;

import dev.crm.module.sedi.dto.SedeCampagnaDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class SedeCampagnaDao
{

  private final DataSource dataSource;

  public SedeCampagnaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long associa(Long sedeId, Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO sedi_campagne (sede_id, campagna_id, created_at) VALUES (?, ?, ?)";
      db.query(sql, sedeId, campagnaId, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int rimuovi(Long sedeId, Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM sedi_campagne WHERE sede_id = ? AND campagna_id = ?";
      return db.query(sql, sedeId, campagnaId);
    } finally {
      db.release();
    }
  }

  public List<SedeCampagnaDto> findBySedeId(Long sedeId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<SedeCampagnaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM sedi_campagne WHERE sede_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, sedeId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<SedeCampagnaDto> findByCampagnaId(Long campagnaId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<SedeCampagnaDto> result = new ArrayList<>();
      String sql = "SELECT * FROM sedi_campagne WHERE campagna_id = ? ORDER BY created_at DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, campagnaId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private SedeCampagnaDto mapRecord(HashMap<String, Object> r)
  {
    return new SedeCampagnaDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("sede_id")),
        DB.toLong(r.get("campagna_id")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
