package dev.crm.module.agenti.dao;

import dev.crm.module.agenti.entity.AppuntamentoEntity;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class AppuntamentoDao
{
  private final DataSource dataSource;

  public AppuntamentoDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * INSERT di un appuntamento
   * @param dto dati dell'appuntamento da inserire
   * @return ID generato
   */
  public long insert(AppuntamentoEntity dto) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "INSERT INTO agenti_appuntamenti (" +
            "agente_id, contatto_id, data_ora, durata_minuti, " +
            "note, stato, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
      db.query(
        sql,
        dto.agenteId,
        dto.contattoId,
        DB.toSqlTimestamp(dto.dataOra),
        dto.durataMinuti != null ? dto.durataMinuti : 30,
        dto.note,
        dto.stato != null ? dto.stato : "PROGRAMMATO",
        DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE di un appuntamento in base all'ID
   * @param dto dati aggiornati dell'appuntamento
   * @return numero di righe aggiornate
   */
  public int update(AppuntamentoEntity dto) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE agenti_appuntamenti SET " +
            "agente_id = ?, contatto_id = ?, data_ora = ?, " +
            "durata_minuti = ?, note = ?, stato = ?, updated_at = ? " +
            "WHERE id = ?";
      return db.query(
        sql,
        dto.agenteId,
        dto.contattoId,
        DB.toSqlTimestamp(dto.dataOra),
        dto.durataMinuti,
        dto.note,
        dto.stato,
        DB.toSqlTimestamp(LocalDateTime.now()),
        dto.id);
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di un appuntamento in base all'ID
   * @param id ID appuntamento
   * @return numero di righe eliminate
   */
  public int delete(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM agenti_appuntamenti WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di un appuntamento in base all'ID
   * @param id ID appuntamento
   * @return appuntamento trovato o null
   */
  public AppuntamentoEntity findById(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT * FROM agenti_appuntamenti WHERE id = ?";
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
   * SELECT degli appuntamenti di un agente ordinati per data decrescente
   * @param agenteId ID agente
   * @return lista di appuntamenti
   */
  public List<AppuntamentoEntity> findByAgenteId(Long agenteId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti " +
            "WHERE agente_id = ? ORDER BY data_ora DESC";
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
   * SELECT degli appuntamenti di un agente in un intervallo di date
   * @param agenteId ID agente
   * @param dataInizio inizio intervallo (incluso)
   * @param dataFine fine intervallo (escluso)
   * @return lista di appuntamenti ordinati per data
   */
  public List<AppuntamentoEntity>
  findByAgenteIdAndData(Long agenteId,
                        LocalDateTime dataInizio,
                        LocalDateTime dataFine) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti " +
            "WHERE agente_id = ? AND data_ora >= ? AND data_ora < ? " +
            "ORDER BY data_ora";
      rs = db.select(sql, agenteId, DB.toSqlTimestamp(dataInizio), DB.toSqlTimestamp(dataFine));
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di tutti gli appuntamenti ordinati per data decrescente
   * @return lista di appuntamenti
   */
  public List<AppuntamentoEntity> findAll() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti ORDER BY data_ora DESC";
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
   * SELECT di tutti gli appuntamenti paginati ordinati per data decrescente
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di appuntamenti
   */
  public List<AppuntamentoEntity> findAll(int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti ORDER BY data_ora DESC LIMIT ? OFFSET ?";
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
   * SELECT del conteggio totale degli appuntamenti
   * @return numero totale di appuntamenti
   */
  public int count() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti_appuntamenti";
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
   * SELECT degli appuntamenti di un agente paginati ordinati per data decrescente
   * @param agenteId ID agente
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di appuntamenti
   */
  public List<AppuntamentoEntity> findByAgenteId(Long agenteId, int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti " +
            "WHERE agente_id = ? ORDER BY data_ora DESC LIMIT ? OFFSET ?";
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
   * SELECT del conteggio degli appuntamenti di un agente
   * @param agenteId ID agente
   * @return numero di appuntamenti dell'agente
   */
  public int countByAgenteId(Long agenteId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti_appuntamenti WHERE agente_id = ?";
      rs = db.select(sql, agenteId);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT degli appuntamenti di un agente in un intervallo di date paginati
   * @param agenteId ID agente
   * @param dataInizio inizio intervallo (incluso)
   * @param dataFine fine intervallo (escluso)
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di appuntamenti ordinati per data
   */
  public List<AppuntamentoEntity>
  findByAgenteIdAndData(Long agenteId,
                        LocalDateTime dataInizio,
                        LocalDateTime dataFine,
                        int limit,
                        int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<AppuntamentoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM agenti_appuntamenti " +
            "WHERE agente_id = ? AND data_ora >= ? AND data_ora < ? " +
            "ORDER BY data_ora LIMIT ? OFFSET ?";
      rs = db.select(sql,
          agenteId,
          DB.toSqlTimestamp(dataInizio),
          DB.toSqlTimestamp(dataFine),
          limit,
          offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio degli appuntamenti di un agente in un intervallo di date
   * @param agenteId ID agente
   * @param dataInizio inizio intervallo (incluso)
   * @param dataFine fine intervallo (escluso)
   * @return numero di appuntamenti nell'intervallo
   */
  public int countByAgenteIdAndData(Long agenteId,
                                    LocalDateTime dataInizio,
                                    LocalDateTime dataFine) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM agenti_appuntamenti " +
            "WHERE agente_id = ? AND data_ora >= ? AND data_ora < ?";
      rs = db.select(sql,
          agenteId,
          DB.toSqlTimestamp(dataInizio),
          DB.toSqlTimestamp(dataFine));
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private AppuntamentoEntity mapRecord(HashMap<String, Object> r)
  {
    return new AppuntamentoEntity(
      DB.toLong(r.get("id")),
      DB.toLong(r.get("agente_id")),
      DB.toLong(r.get("contatto_id")),
      DB.toLocalDateTime(r.get("data_ora")),
      DB.toInteger(r.get("durata_minuti")),
      DB.toString(r.get("note")),
      DB.toString(r.get("stato")),
      DB.toLocalDateTime(r.get("created_at")),
      DB.toLocalDateTime(r.get("updated_at")));
  }
}
