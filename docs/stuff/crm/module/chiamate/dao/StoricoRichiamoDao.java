package dev.crm.module.chiamate.dao;

import dev.crm.module.chiamate.dto.StoricoRichiamoDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class StoricoRichiamoDao
{

  private final DataSource dataSource;

  public StoricoRichiamoDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(Long richiamoId, Long operatoreId, String azione, String dettagli)
      throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO storico_richiami (richiamo_id, operatore_id, azione, dettagli, timestamp) "
          + "VALUES (?, ?, ?, ?, ?)";
      db.query(
          sql, richiamoId, operatoreId, azione, dettagli, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public List<StoricoRichiamoDto> findByRichiamoId(Long richiamoId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<StoricoRichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM storico_richiami WHERE richiamo_id = ? ORDER BY timestamp DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, richiamoId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<StoricoRichiamoDto> findByOperatoreId(Long operatoreId, Integer limit)
      throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<StoricoRichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM storico_richiami WHERE operatore_id = ? ORDER BY timestamp DESC";
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

  private StoricoRichiamoDto mapRecord(HashMap<String, Object> r)
  {
    return new StoricoRichiamoDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("richiamo_id")),
        DB.toLong(r.get("operatore_id")),
        DB.toString(r.get("azione")),
        DB.toString(r.get("dettagli")),
        DB.toLocalDateTime(r.get("timestamp")));
  }
}
