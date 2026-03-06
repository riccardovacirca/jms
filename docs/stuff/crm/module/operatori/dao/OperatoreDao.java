package dev.crm.module.operatori.dao;

import dev.crm.module.operatori.dto.OperatoreDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class OperatoreDao
{

  private final DataSource dataSource;

  public OperatoreDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(OperatoreDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO operatori (nome, cognome, username, email, telefono, stato_attuale, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.nome,
          dto.cognome,
          dto.username,
          dto.email,
          dto.telefono,
          dto.statoAttuale != null ? dto.statoAttuale : "OFFLINE",
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int update(OperatoreDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE operatori SET nome = ?, cognome = ?, username = ?, email = ?, "
          + "telefono = ?, stato_attuale = ?, updated_at = ? WHERE id = ?";
      return db.query(
          sql,
          dto.nome,
          dto.cognome,
          dto.username,
          dto.email,
          dto.telefono,
          dto.statoAttuale,
          DB.toSqlTimestamp(LocalDateTime.now()),
          dto.id);
    } finally {
      db.release();
    }
  }

  public int delete(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "DELETE FROM operatori WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  public Optional<OperatoreDto> findById(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "SELECT * FROM operatori WHERE id = ?";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, id);
      if (rs.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(mapRecord(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public List<OperatoreDto> findAll() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<OperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori ORDER BY cognome, nome";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public int updateStatoAttuale(Long id, String stato) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE operatori SET stato_attuale = ?, updated_at = ? WHERE id = ?";
      return db.query(sql, stato, DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  public List<OperatoreDto> findByStato(String stato) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<OperatoreDto> result = new ArrayList<>();
      String sql = "SELECT * FROM operatori WHERE stato_attuale = ? ORDER BY cognome, nome";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, stato);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public int count() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "SELECT COUNT(*) as cnt FROM operatori";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private OperatoreDto mapRecord(HashMap<String, Object> r)
  {
    return new OperatoreDto(
        DB.toLong(r.get("id")),
        DB.toString(r.get("nome")),
        DB.toString(r.get("cognome")),
        DB.toString(r.get("username")),
        DB.toString(r.get("email")),
        DB.toString(r.get("telefono")),
        DB.toString(r.get("stato_attuale")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));
  }
}
