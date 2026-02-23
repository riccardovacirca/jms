package dev.crm.module.campagne.dao;

import dev.crm.module.campagne.entity.CampagnaEntity;
import dev.crm.module.campagne.entity.CampagnaListaEntity;
import dev.springtools.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class CampagnaDao
{
  private final DataSource dataSource;

  public CampagnaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * INSERT di una campagna
   * @param entity dati della campagna da inserire
   * @return ID generato
   */
  public long insert(CampagnaEntity entity) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "INSERT INTO campagne (" +
            "nome, descrizione, tipo, stato, data_inizio, " +
            "data_fine, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          entity.nome,
          entity.descrizione,
          entity.tipo != null ? entity.tipo : "outbound",
          entity.stato != null ? entity.stato : 1,
          entity.dataInizio != null ? DB.toSqlDate(entity.dataInizio) : null,
          entity.dataFine != null ? DB.toSqlDate(entity.dataFine) : null,
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE di una campagna in base all'ID
   * @param entity dati aggiornati della campagna
   * @return numero di righe aggiornate
   */
  public int update(CampagnaEntity entity) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE campagne SET " +
            "nome = ?, descrizione = ?, tipo = ?, stato = ?, " +
            "data_inizio = ?, data_fine = ?, updated_at = ? " +
            "WHERE id = ?";
      return db.query(
          sql,
          entity.nome,
          entity.descrizione,
          entity.tipo,
          entity.stato,
          entity.dataInizio != null ? DB.toSqlDate(entity.dataInizio) : null,
          entity.dataFine != null ? DB.toSqlDate(entity.dataFine) : null,
          DB.toSqlTimestamp(LocalDateTime.now()),
          entity.id);
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di una campagna in base all'ID
   * @param id ID campagna
   * @return numero di righe eliminate
   */
  public int delete(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM campagne WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di una campagna in base all'ID
   * @param id ID campagna
   * @return campagna trovata o null
   */
  public CampagnaEntity findById(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT * FROM campagne WHERE id = ?";
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
   * SELECT di tutte le campagne ordinate per ID decrescente
   * @return lista di campagne
   */
  public List<CampagnaEntity> findAll() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<CampagnaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM campagne ORDER BY id DESC";
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
   * UPDATE dello stato di una campagna in base all'ID
   * @param id ID campagna
   * @param stato nuovo stato da impostare
   * @return numero di righe aggiornate
   */
  public int updateStato(Long id, Integer stato) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE campagne SET stato = ?, updated_at = ? WHERE id = ?";
      return db.query(sql, stato, DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio totale delle campagne
   * @return numero totale di campagne
   */
  public int count() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM campagne";
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
   * SELECT di tutte le campagne paginate ordinate per ID decrescente
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di campagne
   */
  public List<CampagnaEntity> findAll(int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<CampagnaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM campagne ORDER BY id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  // --- Gestione liste campagna ---

  /**
   * INSERT di una lista in una campagna
   * @param campagnaId ID campagna
   * @param listaId ID lista
   * @return ID generato
   */
  public long addLista(Long campagnaId, Long listaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "INSERT OR IGNORE " +
            "INTO campagna_liste (campagna_id, lista_id, created_at) " +
            "VALUES (?, ?, ?)";
      db.query(sql, campagnaId, listaId, DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di una lista da una campagna
   * @param campagnaId ID campagna
   * @param listaId ID lista
   * @return numero di righe eliminate
   */
  public int removeLista(Long campagnaId, Long listaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM campagna_liste WHERE campagna_id = ? AND lista_id = ?";
      return db.query(sql, campagnaId, listaId);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT delle liste associate a una campagna ordinate per data decrescente
   * @param campagnaId ID campagna
   * @return lista di associazioni campagna-lista
   */
  public List<CampagnaListaEntity>
  findListeByCampagnaId(Long campagnaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<CampagnaListaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM campagna_liste " +
            "WHERE campagna_id = ? ORDER BY created_at DESC";
      rs = db.select(sql, campagnaId);
      for (HashMap<String, Object> r : rs) {
        result.add(
            new CampagnaListaEntity(
                DB.toLong(r.get("id")),
                DB.toLong(r.get("campagna_id")),
                DB.toLong(r.get("lista_id")),
                DB.toLocalDateTime(r.get("created_at"))));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT delle liste associate a una campagna paginate ordinate per data decrescente
   * @param campagnaId ID campagna
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di associazioni campagna-lista
   */
  public List<CampagnaListaEntity>
  findListeByCampagnaId(Long campagnaId, int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<CampagnaListaEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM campagna_liste " +
            "WHERE campagna_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, campagnaId, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(
            new CampagnaListaEntity(
                DB.toLong(r.get("id")),
                DB.toLong(r.get("campagna_id")),
                DB.toLong(r.get("lista_id")),
                DB.toLocalDateTime(r.get("created_at"))));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio delle liste associate a una campagna
   * @param campagnaId ID campagna
   * @return numero di liste associate alla campagna
   */
  public int countListeByCampagnaId(Long campagnaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM campagna_liste WHERE campagna_id = ?";
      rs = db.select(sql, campagnaId);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  private CampagnaEntity mapRecord(HashMap<String, Object> r)
  {
    return new CampagnaEntity(
        DB.toLong(r.get("id")),
        DB.toString(r.get("nome")),
        DB.toString(r.get("descrizione")),
        DB.toString(r.get("tipo")),
        DB.toInteger(r.get("stato")),
        DB.toLocalDate(r.get("data_inizio")),
        DB.toLocalDate(r.get("data_fine")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));
  }
}
