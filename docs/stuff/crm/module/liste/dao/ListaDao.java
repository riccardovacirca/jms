package dev.crm.module.liste.dao;

import dev.crm.module.liste.dto.ListaContattoDto;
import dev.crm.module.liste.dto.ListaDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class ListaDao
{
  private final DataSource dataSource;

  public ListaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(ListaDto dto) throws Exception
  {
    DB db;
    String sql;
    Boolean consenso;
    Integer stato;
    Object scadenzaValue;
    long result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "INSERT INTO liste (nome, descrizione, consenso, stato, scadenza, created_at) VALUES (?, ?, ?, ?, ?, ?)";
      consenso = dto.consenso != null ? dto.consenso : false;
      stato = dto.stato != null ? dto.stato : 1;
      scadenzaValue = dto.scadenza != null ? DB.toSqlDate(dto.scadenza) : null;
      db.query(
          sql,
          dto.nome,
          dto.descrizione,
          consenso,
          stato,
          scadenzaValue,
          DB.toSqlTimestamp(LocalDateTime.now()));
      result = db.lastInsertId();
      return result;
    } finally {
      db.release();
    }
  }

  public int update(ListaDto dto) throws Exception
  {
    DB db;
    String sql;
    Object scadenzaValue;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE liste SET nome = ?, descrizione = ?, consenso = ?, stato = ?, scadenza = ?, updated_at = ? WHERE id = ?";
      scadenzaValue = dto.scadenza != null ? DB.toSqlDate(dto.scadenza) : null;
      result = db.query(
          sql,
          dto.nome,
          dto.descrizione,
          dto.consenso,
          dto.stato,
          scadenzaValue,
          DB.toSqlTimestamp(LocalDateTime.now()),
          dto.id);
      return result;
    } finally {
      db.release();
    }
  }

  public int delete(Long id) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE liste SET deleted_at = ?, updated_at = ? WHERE id = ?";
      result = db.query(
          sql, DB.toSqlTimestamp(LocalDateTime.now()), DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  public Optional<ListaDto> findById(Long id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<ListaDto> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT l.*, "
          + "(SELECT COUNT(*) FROM lista_contatti lc WHERE lc.lista_id = l.id) as contatti_count "
          + "FROM liste l WHERE l.id = ? AND l.deleted_at IS NULL";
      rs = db.select(sql, id);
      if (rs.isEmpty()) {
        result = Optional.empty();
        return result;
      }
      result = Optional.of(mapRecord(rs.get(0)));
      return result;
    } finally {
      db.release();
    }
  }

  public List<ListaDto> findAll() throws Exception
  {
    DB db;
    List<ListaDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT l.*, "
          + "(SELECT COUNT(*) FROM lista_contatti lc WHERE lc.lista_id = l.id) as contatti_count "
          + "FROM liste l WHERE l.deleted_at IS NULL ORDER BY l.id DESC";
      rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<ListaDto> findAll(int limit, int offset) throws Exception
  {
    DB db;
    List<ListaDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT l.*, "
          + "(SELECT COUNT(*) FROM lista_contatti lc WHERE lc.lista_id = l.id) as contatti_count "
          + "FROM liste l WHERE l.deleted_at IS NULL ORDER BY l.id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<ListaDto> search(String query, int limit, int offset) throws Exception
  {
    DB db;
    List<ListaDto> result;
    String pattern;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      pattern = "%" + query + "%";
      sql = "SELECT l.*, "
          + "(SELECT COUNT(*) FROM lista_contatti lc WHERE lc.lista_id = l.id) as contatti_count "
          + "FROM liste l WHERE l.deleted_at IS NULL AND (l.nome LIKE ? OR l.descrizione LIKE ?) ORDER BY l.id DESC LIMIT ? OFFSET ?";
      rs = db.select(sql, pattern, pattern, limit, offset);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public int countSearch(String query) throws Exception
  {
    DB db;
    String pattern;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      pattern = "%" + query + "%";
      sql = "SELECT COUNT(*) as cnt FROM liste WHERE deleted_at IS NULL AND (nome LIKE ? OR descrizione LIKE ?)";
      rs = db.select(sql, pattern, pattern);
      if (!rs.isEmpty()) {
        result = DB.toInteger(rs.get(0).get("cnt"));
        return result;
      }
      result = 0;
      return result;
    } finally {
      db.release();
    }
  }

  public int updateStato(Long id, Integer stato) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE liste SET stato = ?, updated_at = ? WHERE id = ?";
      result = db.query(sql, stato, DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  public int updateScadenza(Long id, java.time.LocalDate scadenza) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE liste SET scadenza = ?, updated_at = ? WHERE id = ?";
      result = db.query(sql, DB.toSqlDate(scadenza), DB.toSqlTimestamp(LocalDateTime.now()), id);
      return result;
    } finally {
      db.release();
    }
  }

  public int count() throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM liste WHERE deleted_at IS NULL";
      rs = db.select(sql);
      if (!rs.isEmpty()) {
        result = DB.toInteger(rs.get(0).get("cnt"));
        return result;
      }
      result = 0;
      return result;
    } finally {
      db.release();
    }
  }

  /** Verifica se esiste già una lista con questo nome (escludendo quella in modifica) */
  public boolean existsByNome(String nome, Long excludeId) throws Exception
  {
    DB db;
    String sql;
    Object[] params;
    ArrayList<HashMap<String, Object>> rs;
    int count;
    boolean result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM liste WHERE nome = ? AND deleted_at IS NULL";

      if (excludeId != null) {
        sql = sql + " AND id != ?";
        params = new Object[]{nome, excludeId};
      } else {
        params = new Object[]{nome};
      }

      rs = db.select(sql, params);
      if (!rs.isEmpty()) {
        count = DB.toInteger(rs.get(0).get("cnt"));
        result = count > 0;
        return result;
      }
      result = false;
      return result;
    } finally {
      db.release();
    }
  }

  // --- Gestione contatti lista ---

  public long addContatto(Long listaId, Long contattoId) throws Exception
  {
    DB db;
    String sql;
    long result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "INSERT INTO lista_contatti (lista_id, contatto_id, created_at) VALUES (?, ?, ?)";
      db.query(sql, listaId, contattoId, DB.toSqlTimestamp(LocalDateTime.now()));
      result = db.lastInsertId();
      return result;
    } finally {
      db.release();
    }
  }

  public int removeContatto(Long listaId, Long contattoId) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "DELETE FROM lista_contatti WHERE lista_id = ? AND contatto_id = ?";
      result = db.query(sql, listaId, contattoId);
      return result;
    } finally {
      db.release();
    }
  }

  public List<ListaContattoDto> findContattiByListaId(Long listaId) throws Exception
  {
    DB db;
    List<ListaContattoDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    ListaContattoDto dto;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT lc.*, "
          + "COALESCE("
          + "  NULLIF(TRIM(c.ragione_sociale), ''), "
          + "  NULLIF(TRIM(COALESCE(c.cognome, '') || ' ' || COALESCE(c.nome, '')), ''), "
          + "  c.telefono"
          + ") as nome_contatto, "
          + "c.telefono "
          + "FROM lista_contatti lc "
          + "LEFT JOIN contatti c ON lc.contatto_id = c.id "
          + "WHERE lc.lista_id = ? "
          + "ORDER BY lc.created_at DESC";
      rs = db.select(sql, listaId);
      for (HashMap<String, Object> r : rs) {
        dto = new ListaContattoDto(
            DB.toLong(r.get("id")),
            DB.toLong(r.get("lista_id")),
            DB.toLong(r.get("contatto_id")),
            DB.toLocalDateTime(r.get("created_at")),
            DB.toString(r.get("nome_contatto")),
            DB.toString(r.get("telefono")));
        result.add(dto);
      }
      return result;
    } finally {
      db.release();
    }
  }

  private ListaDto mapRecord(HashMap<String, Object> r)
  {
    ListaDto dto;
    Long contattiCount;
    Integer stato;

    dto = new ListaDto(
        DB.toLong(r.get("id")),
        DB.toString(r.get("nome")),
        DB.toString(r.get("descrizione")),
        DB.toBoolean(r.get("consenso")),
        DB.toInteger(r.get("stato")),
        DB.toLocalDate(r.get("scadenza")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")),
        DB.toLocalDateTime(r.get("deleted_at")));

    // Popola campi aggiuntivi
    contattiCount = DB.toLong(r.get("contatti_count"));
    dto.contattiCount = contattiCount != null ? contattiCount : 0L;

    stato = DB.toInteger(r.get("stato"));
    dto.attiva = stato != null && stato == 1; // Stato 1 = attiva

    return dto;
  }
}
