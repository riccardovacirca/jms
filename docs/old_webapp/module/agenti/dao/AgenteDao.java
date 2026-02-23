package dev.crm.module.agenti.dao;

import dev.crm.module.agenti.entity.AgenteEntity;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class AgenteDao
{
  private final DataSource dataSource;

  public AgenteDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * INSERT di un agente
   * @param dto dati dell'agente da inserire
   * @return ID generato
   */
  public long insert(AgenteEntity dto) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "INSERT INTO agenti (" +
            "nome, cognome, email, telefono, note, " +
            "attivo, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
      db.query(
        sql,
        dto.nome,
        dto.cognome,
        dto.email,
        dto.telefono,
        dto.note,
        dto.attivo != null ? dto.attivo : 1,
        DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE di un agente in base all'ID
   * @param dto dati aggiornati dell'agente
   * @return numero di righe aggiornate
   */
  public int update(AgenteEntity dto) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE agenti SET " +
            "nome = ?, cognome = ?, email = ?, telefono = ?, " +
            "note = ?, attivo = ?, updated_at = ? " +
            "WHERE id = ?";
      return db.query(
        sql,
        dto.nome,
        dto.cognome,
        dto.email,
        dto.telefono,
        dto.note,
        dto.attivo,
        DB.toSqlTimestamp(LocalDateTime.now()),
        dto.id);
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di un agente in base all'ID
   * @param id ID agente
   * @return numero di righe eliminate
   */
  public int delete(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM agenti WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di un agente in base all'ID
   * @param id ID agente
   * @return agente trovato o null
   */
  public AgenteEntity findById(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT * FROM agenti WHERE id = ?";
      rs = db.select(sql, id);
      if (rs.isEmpty()) {
        return null;
      }
      return mapRecord(rs.get(0));
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di tutti gli agenti ordinati per cognome e nome
   * @return lista di agenti
   */
  public List<AgenteEntity> findAll() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AgenteEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti ORDER BY cognome, nome";
      rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT degli agenti filtrati per stato attivo
   * @param attivo stato attivo (1 = attivo, 0 = inattivo)
   * @return lista di agenti
   */
  public List<AgenteEntity> findByAttivo(Integer attivo) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AgenteEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti " +
            "WHERE attivo = ? ORDER BY cognome, nome";
      rs = db.select(sql, attivo);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio totale degli agenti
   * @return numero totale di agenti
   */
  public int count() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti";
      rs = db.select(sql);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di tutti gli agenti paginati ordinati per cognome e nome
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di agenti
   */
  public List<AgenteEntity> findAll(int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AgenteEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti ORDER BY cognome, nome LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT degli agenti filtrati per stato attivo paginati
   * @param attivo stato attivo (1 = attivo, 0 = inattivo)
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di agenti
   */
  public List<AgenteEntity> findByAttivo(Integer attivo, int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AgenteEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti WHERE attivo = ? ORDER BY cognome, nome LIMIT ? OFFSET ?";
      rs = db.select(sql, attivo, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio degli agenti filtrati per stato attivo
   * @param attivo stato attivo (1 = attivo, 0 = inattivo)
   * @return numero di agenti con lo stato specificato
   */
  public int countByAttivo(Integer attivo) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti WHERE attivo = ?";
      rs = db.select(sql, attivo);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private AgenteEntity mapRecord(HashMap<String, Object> r)
  {
    return new AgenteEntity(
      DB.toLong(r.get("id")),
      DB.toString(r.get("nome")),
      DB.toString(r.get("cognome")),
      DB.toString(r.get("email")),
      DB.toString(r.get("telefono")),
      DB.toString(r.get("note")),
      DB.toInteger(r.get("attivo")),
      DB.toLocalDateTime(r.get("created_at")),
      DB.toLocalDateTime(r.get("updated_at")));
  }
}
