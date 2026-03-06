package dev.crm.module.chiamate.dao;

import dev.crm.module.chiamate.dto.RichiamoDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class RichiamoDao
{

  private final DataSource dataSource;

  public RichiamoDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(RichiamoDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO richiami (operatore_id, contatto_id, data_ora, durata_minuti, note, stato, campagna_id, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.operatoreId,
          dto.contattoId,
          DB.toSqlTimestamp(dto.dataOra),
          dto.durataMinuti != null ? dto.durataMinuti : 5,
          dto.note,
          dto.stato != null ? dto.stato : "PROGRAMMATO",
          dto.campagnaId,
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int update(RichiamoDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE richiami SET operatore_id = ?, contatto_id = ?, data_ora = ?, "
          + "durata_minuti = ?, note = ?, stato = ?, campagna_id = ?, updated_at = ? WHERE id = ?";
      return db.query(
          sql,
          dto.operatoreId,
          dto.contattoId,
          DB.toSqlTimestamp(dto.dataOra),
          dto.durataMinuti,
          dto.note,
          dto.stato,
          dto.campagnaId,
          DB.toSqlTimestamp(LocalDateTime.now()),
          dto.id);
    } finally {
      db.release();
    }
  }

  public int updateStato(Long id, String stato) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE richiami SET stato = ?, updated_at = ? WHERE id = ?";
      return db.query(sql, stato, DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  public int updateDataOra(Long id, LocalDateTime nuovaDataOra) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE richiami SET data_ora = ?, updated_at = ? WHERE id = ?";
      return db.query(
          sql, DB.toSqlTimestamp(nuovaDataOra), DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  public int delete(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM richiami WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  public Optional<RichiamoDto> findById(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "SELECT * FROM richiami WHERE id = ?";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, id);
      if (rs.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(mapRecord(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public List<RichiamoDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<RichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM richiami WHERE operatore_id = ? ORDER BY data_ora ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<RichiamoDto> findByOperatoreIdAndStato(Long operatoreId, String stato)
      throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<RichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM richiami WHERE operatore_id = ? AND stato = ? ORDER BY data_ora ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId, stato);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<RichiamoDto> findImminenti(Long operatoreId, LocalDateTime dataOraLimite)
      throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<RichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM richiami WHERE operatore_id = ? AND stato = 'PROGRAMMATO' AND data_ora <= ? ORDER BY data_ora ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, operatoreId, DB.toSqlTimestamp(dataOraLimite));
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<RichiamoDto> findAll() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<RichiamoDto> result = new ArrayList<>();
      String sql = "SELECT * FROM richiami ORDER BY data_ora DESC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private RichiamoDto mapRecord(HashMap<String, Object> r)
  {
    return new RichiamoDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toLong(r.get("contatto_id")),
        DB.toLocalDateTime(r.get("data_ora")),
        DB.toInteger(r.get("durata_minuti")),
        DB.toString(r.get("note")),
        DB.toString(r.get("stato")),
        DB.toLong(r.get("campagna_id")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));
  }
}
