package dev.crm.module.sedi.dao;

import dev.crm.module.sedi.dto.SedeDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class SedeDao
{

  private final DataSource dataSource;

  public SedeDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(SedeDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO sedi (nome, indirizzo, citta, cap, telefono, email, note, attiva, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.nome,
          dto.indirizzo,
          dto.citta,
          dto.cap,
          dto.telefono,
          dto.email,
          dto.note,
          dto.attiva != null ? dto.attiva : 1,
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public int update(SedeDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "UPDATE sedi SET nome = ?, indirizzo = ?, citta = ?, cap = ?, "
          + "telefono = ?, email = ?, note = ?, attiva = ?, updated_at = ? WHERE id = ?";
      return db.query(
          sql,
          dto.nome,
          dto.indirizzo,
          dto.citta,
          dto.cap,
          dto.telefono,
          dto.email,
          dto.note,
          dto.attiva,
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
      String sql = "DELETE FROM sedi WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  public Optional<SedeDto> findById(Long id) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "SELECT * FROM sedi WHERE id = ?";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, id);
      if (rs.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(mapRecord(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public List<SedeDto> findAll() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<SedeDto> result = new ArrayList<>();
      String sql = "SELECT * FROM sedi ORDER BY nome ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
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
      String sql = "SELECT COUNT(*) as cnt FROM sedi";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private SedeDto mapRecord(HashMap<String, Object> r)
  {
    return new SedeDto(
        DB.toLong(r.get("id")),
        DB.toString(r.get("nome")),
        DB.toString(r.get("indirizzo")),
        DB.toString(r.get("citta")),
        DB.toString(r.get("cap")),
        DB.toString(r.get("telefono")),
        DB.toString(r.get("email")),
        DB.toString(r.get("note")),
        DB.toInteger(r.get("attiva")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));
  }
}
