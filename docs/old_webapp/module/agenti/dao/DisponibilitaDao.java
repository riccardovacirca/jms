package dev.crm.module.agenti.dao;

import dev.crm.module.agenti.entity.DisponibilitaEntity;
import dev.springtools.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class DisponibilitaDao
{
  private final DataSource dataSource;

  public DisponibilitaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * INSERT di una disponibilità per un agente
   * @param dto dati della disponibilità da inserire
   * @return ID generato
   */
  public long insert(DisponibilitaEntity dto) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "INSERT INTO agenti_disponibilita (agente_id, giorno_settimana, ora_inizio, ora_fine, created_at) "
          + "VALUES (?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.agenteId,
          dto.giornoSettimana,
          DB.toSqlTime(dto.oraInizio),
          DB.toSqlTime(dto.oraFine),
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di una disponibilità in base all'ID
   * @param id ID disponibilità
   * @return numero di righe eliminate
   */
  public int delete(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM agenti_disponibilita WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT delle disponibilità di un agente ordinate per giorno e ora
   * @param agenteId ID agente
   * @return lista di disponibilità
   */
  public List<DisponibilitaEntity> findByAgenteId(Long agenteId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<DisponibilitaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_disponibilita WHERE agente_id = ? ORDER BY giorno_settimana, ora_inizio";
      rs = db.select(sql, agenteId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di tutte le disponibilità ordinate per agente, giorno e ora
   * @return lista di disponibilità
   */
  public List<DisponibilitaEntity> findAll() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<DisponibilitaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_disponibilita ORDER BY agente_id, giorno_settimana, ora_inizio";
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
   * SELECT delle disponibilità di un agente paginate ordinate per giorno e ora
   * @param agenteId ID agente
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di disponibilità
   */
  public List<DisponibilitaEntity> findByAgenteId(Long agenteId, int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<DisponibilitaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_disponibilita " +
            "WHERE agente_id = ? ORDER BY giorno_settimana, ora_inizio LIMIT ? OFFSET ?";
      rs = db.select(sql, agenteId, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio delle disponibilità di un agente
   * @param agenteId ID agente
   * @return numero di disponibilità dell'agente
   */
  public int countByAgenteId(Long agenteId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti_disponibilita WHERE agente_id = ?";
      rs = db.select(sql, agenteId);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private DisponibilitaEntity mapRecord(HashMap<String, Object> r)
  {
    return new DisponibilitaEntity(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("agente_id")),
        DB.toInteger(r.get("giorno_settimana")),
        DB.toLocalTime(r.get("ora_inizio")),
        DB.toLocalTime(r.get("ora_fine")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
