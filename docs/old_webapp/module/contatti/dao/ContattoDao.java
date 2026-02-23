package dev.crm.module.contatti.dao;

import dev.crm.module.contatti.entity.ContattoEntity;
import dev.springtools.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class ContattoDao
{
  private final DataSource dataSource;

  public ContattoDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * INSERT di un contatto
   * @param entity dati del contatto da inserire
   * @return ID generato
   */
  public long insert(ContattoEntity entity) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      Integer stato;
      Boolean consenso;
      Boolean blacklist;
      db.acquire();
      sql = "INSERT INTO contatti (" +
            "nome, cognome, ragione_sociale, telefono, email, " +
            "indirizzo, citta, cap, provincia, note, stato, consenso, " +
            "blacklist, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      stato = entity.stato != null ? entity.stato : 1;
      consenso = entity.consenso != null ? entity.consenso : false;
      blacklist = entity.blacklist != null ? entity.blacklist : false;
      db.query(
        sql,
        entity.nome,
        entity.cognome,
        entity.ragioneSociale,
        entity.telefono,
        entity.email,
        entity.indirizzo,
        entity.citta,
        entity.cap,
        entity.provincia,
        entity.note,
        stato,
        consenso,
        blacklist,
        DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE di un contatto in base all'ID
   * @param entity dati aggiornati del contatto
   * @return numero di righe aggiornate
   */
  public int update(ContattoEntity entity) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE contatti SET " +
            "nome = ?, cognome = ?, ragione_sociale = ?, telefono = ?, " +
            "email = ?, indirizzo = ?, citta = ?, cap = ?, provincia = ?, " +
            "note = ?, stato = ?, consenso = ?, blacklist = ?, updated_at = ? " +
            "WHERE id = ?";
      return db.query(
          sql,
          entity.nome,
          entity.cognome,
          entity.ragioneSociale,
          entity.telefono,
          entity.email,
          entity.indirizzo,
          entity.citta,
          entity.cap,
          entity.provincia,
          entity.note,
          entity.stato,
          entity.consenso,
          entity.blacklist,
          DB.toSqlTimestamp(LocalDateTime.now()),
          entity.id);
    } finally {
      db.release();
    }
  }

  /**
   * DELETE di un contatto in base all'ID
   * @param id ID contatto
   * @return numero di righe eliminate
   */
  public int delete(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "DELETE FROM contatti WHERE id = ?";
      return db.query(sql, id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di un contatto in base all'ID
   * @param id ID contatto
   * @return contatto trovato o null
   */
  public ContattoEntity findById(Long id) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT * FROM contatti WHERE id = ?";
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
   * SELECT di tutti i contatti con conteggio liste
   * @return lista di contatti
   */
  public List<ContattoEntity> findAll() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<ContattoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      ContattoEntity entity;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT c.*, COUNT(lc.lista_id) as liste_count "
          + "FROM contatti c "
          + "LEFT JOIN lista_contatti lc ON c.id = lc.contatto_id "
          + "GROUP BY c.id "
          + "ORDER BY c.id DESC";
      rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        entity = mapRecord(r);
        entity.listeCount = DB.toInteger(r.get("liste_count"));
        result.add(entity);
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di tutti i contatti con conteggio liste, paginati
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di contatti
   */
  public List<ContattoEntity> findAll(int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<ContattoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      ContattoEntity entity;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT c.*, COUNT(lc.lista_id) as liste_count "
          + "FROM contatti c "
          + "LEFT JOIN lista_contatti lc ON c.id = lc.contatto_id "
          + "GROUP BY c.id "
          + "ORDER BY c.id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        entity = mapRecord(r);
        entity.listeCount = DB.toInteger(r.get("liste_count"));
        result.add(entity);
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT dei contatti che corrispondono a una query di ricerca
   * @param query testo da cercare in nome, cognome, ragione sociale, telefono, email
   * @param limit numero massimo di risultati
   * @return lista di contatti
   */
  public List<ContattoEntity> search(String query, int limit) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<ContattoEntity> result;
      String pattern;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      ContattoEntity entity;
      db.acquire();
      result = new ArrayList<>();
      pattern = "%" + query + "%";
      sql = "SELECT c.*, COUNT(lc.lista_id) as liste_count " +
            "FROM contatti c " +
            "LEFT JOIN lista_contatti lc ON c.id = lc.contatto_id " +
            "WHERE " +
            "c.nome LIKE ? OR " +
            "c.cognome LIKE ? OR " +
            "c.ragione_sociale LIKE ? OR " +
            "c.telefono LIKE ? OR " +
            "c.email LIKE ? " +
            "GROUP BY c.id " +
            "ORDER BY c.cognome, c.nome LIMIT ?";
      rs = db.select(sql, pattern, pattern, pattern, pattern, pattern, limit);
      for (HashMap<String, Object> r : rs) {
        entity = mapRecord(r);
        entity.listeCount = DB.toInteger(r.get("liste_count"));
        result.add(entity);
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio totale dei contatti
   * @return numero totale di contatti
   */
  public int count() throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM contatti";
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
   * SELECT dei contatti appartenenti a una lista
   * @param listaId ID lista
   * @return lista di contatti
   */
  public List<ContattoEntity> findByListaId(Long listaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<ContattoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      ContattoEntity entity;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT c.*, COUNT(DISTINCT lc2.lista_id) as liste_count "
          + "FROM contatti c "
          + "INNER JOIN lista_contatti lc ON c.id = lc.contatto_id "
          + "LEFT JOIN lista_contatti lc2 ON c.id = lc2.contatto_id "
          + "WHERE lc.lista_id = ? "
          + "GROUP BY c.id "
          + "ORDER BY c.id DESC";
      rs = db.select(sql, listaId);
      for (HashMap<String, Object> r : rs) {
        entity = mapRecord(r);
        entity.listeCount = DB.toInteger(r.get("liste_count"));
        result.add(entity);
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT dei contatti appartenenti a una lista, paginati
   * @param listaId ID lista
   * @param limit numero massimo di risultati
   * @param offset offset di paginazione
   * @return lista di contatti
   */
  public List<ContattoEntity>
  findByListaId(Long listaId, int limit, int offset) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      List<ContattoEntity> result;
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      ContattoEntity entity;
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT c.*, COUNT(DISTINCT lc2.lista_id) as liste_count "
          + "FROM contatti c "
          + "INNER JOIN lista_contatti lc ON c.id = lc.contatto_id "
          + "LEFT JOIN lista_contatti lc2 ON c.id = lc2.contatto_id "
          + "WHERE lc.lista_id = ? "
          + "GROUP BY c.id "
          + "ORDER BY c.id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, listaId, limit, offset);
      for (HashMap<String, Object> r : rs) {
        entity = mapRecord(r);
        entity.listeCount = DB.toInteger(r.get("liste_count"));
        result.add(entity);
      }
      return result;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT del conteggio dei contatti appartenenti a una lista
   * @param listaId ID lista
   * @return numero di contatti nella lista
   */
  public int countByListaId(Long listaId) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM contatti c "
          + "INNER JOIN lista_contatti lc ON c.id = lc.contatto_id "
          + "WHERE lc.lista_id = ?";
      rs = db.select(sql, listaId);
      if (!rs.isEmpty()) {
        return DB.toInteger(rs.get(0).get("cnt"));
      }
      return 0;
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE dello stato di un contatto in base all'ID
   * @param id ID contatto
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
      sql = "UPDATE contatti SET stato = ?, updated_at = ? WHERE id = ?";
      return db.query(sql, stato, DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  /**
   * UPDATE del flag blacklist di un contatto in base all'ID
   * @param id ID contatto
   * @param blacklist valore blacklist da impostare
   * @return numero di righe aggiornate
   */
  public int setBlacklist(Long id, Boolean blacklist) throws Exception
  {
    DB db;
    db = new DB(dataSource);
    try {
      String sql;
      db.acquire();
      sql = "UPDATE contatti SET blacklist = ?, updated_at = ? WHERE id = ?";
      return db.query(sql, blacklist, DB.toSqlTimestamp(LocalDateTime.now()), id);
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di verifica esistenza contatto per numero di telefono
   * @param telefono numero di telefono da cercare
   * @return true se esiste almeno un contatto con questo telefono
   */
  public boolean existsByTelefono(String telefono) throws Exception
  {
    DB db;
    if (telefono == null || telefono.trim().isEmpty()) {
      return false;
    }
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      int count;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM contatti WHERE telefono = ?";
      rs = db.select(sql, telefono);
      if (!rs.isEmpty()) {
        count = DB.toInteger(rs.get(0).get("cnt"));
        return count > 0;
      }
      return false;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT di verifica blacklist per numero di telefono
   * @param telefono numero di telefono da verificare
   * @return true se il numero è in blacklist
   */
  public boolean isInBlacklist(String telefono) throws Exception
  {
    DB db;
    if (telefono == null || telefono.trim().isEmpty()) {
      return false;
    }
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      int count;
      db.acquire();
      sql = "SELECT COUNT(*) as cnt " +
            "FROM contatti WHERE telefono = ? AND blacklist = 1";
      rs = db.select(sql, telefono);
      if (!rs.isEmpty()) {
        count = DB.toInteger(rs.get(0).get("cnt"));
        return count > 0;
      }
      return false;
    } finally {
      db.release();
    }
  }

  /**
   * SELECT dell'ID di un contatto dato il numero di telefono
   * @param telefono numero di telefono da cercare
   * @return ID del contatto o null se non trovato
   */
  public Long findIdByTelefono(String telefono) throws Exception
  {
    DB db;
    if (telefono == null || telefono.trim().isEmpty()) {
      return null;
    }
    db = new DB(dataSource);
    try {
      String sql;
      ArrayList<HashMap<String, Object>> rs;
      db.acquire();
      sql = "SELECT id FROM contatti WHERE telefono = ? LIMIT 1";
      rs = db.select(sql, telefono);
      if (!rs.isEmpty()) {
        return DB.toLong(rs.get(0).get("id"));
      }
      return null;
    } finally {
      db.release();
    }
  }

  private ContattoEntity mapRecord(HashMap<String, Object> r)
  {
    return new ContattoEntity(
      DB.toLong(r.get("id")),
      DB.toString(r.get("nome")),
      DB.toString(r.get("cognome")),
      DB.toString(r.get("ragione_sociale")),
      DB.toString(r.get("telefono")),
      DB.toString(r.get("email")),
      DB.toString(r.get("indirizzo")),
      DB.toString(r.get("citta")),
      DB.toString(r.get("cap")),
      DB.toString(r.get("provincia")),
      DB.toString(r.get("note")),
      DB.toInteger(r.get("stato")),
      DB.toBoolean(r.get("consenso")),
      DB.toBoolean(r.get("blacklist")),
      DB.toLocalDateTime(r.get("created_at")),
      DB.toLocalDateTime(r.get("updated_at")));
  }
}
