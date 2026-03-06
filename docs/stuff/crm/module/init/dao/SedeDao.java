package dev.crm.module.init.dao;

import dev.crm.module.init.dto.SedeDto;
import dev.springtools.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;

public class SedeDao
{
  private final DataSource dataSource;

  public SedeDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public List<SedeDto> findAll() throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    List<SedeDto> list;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT * FROM sedi ORDER BY nome");
      list = new ArrayList<>();
      for (int i = 0; i < rs.size(); i++) {
        list.add(mapToDto(rs.get(i)));
      }
      return list;
    } finally {
      db.release();
    }
  }

  public Integer count() throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    Object cntObj;
    Integer count;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT COUNT(*) as cnt FROM sedi");
      cntObj = rs.get(0).get("cnt");
      count = cntObj instanceof Long ? ((Long) cntObj).intValue() : (Integer) cntObj;
      return count;
    } finally {
      db.release();
    }
  }

  public Long insert(SedeDto dto) throws Exception
  {
    DB db;
    Long id;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      db.query(
          "INSERT INTO sedi (nome, indirizzo, cap, citta, provincia, nazione, "
              + "numero_postazioni, responsabile_nome, telefono, email, attiva) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          dto.nome,
          dto.indirizzo,
          dto.cap,
          dto.citta,
          dto.provincia,
          dto.nazione != null ? dto.nazione : "Italia",
          dto.numeroPostazioni,
          dto.responsabileNome,
          dto.telefono,
          dto.email,
          dto.attiva != null ? dto.attiva : 1);

      id = db.lastInsertId();
      db.commit();
      return id;
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }

  private SedeDto mapToDto(java.util.Map<String, Object> row)
  {
    SedeDto dto;
    Object idObj;

    dto = new SedeDto();
    idObj = row.get("id");
    dto.id = idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    dto.nome = (String) row.get("nome");
    dto.indirizzo = (String) row.get("indirizzo");
    dto.cap = (String) row.get("cap");
    dto.citta = (String) row.get("citta");
    dto.provincia = (String) row.get("provincia");
    dto.nazione = (String) row.get("nazione");
    dto.numeroPostazioni = (Integer) row.get("numero_postazioni");
    dto.responsabileNome = (String) row.get("responsabile_nome");
    dto.telefono = (String) row.get("telefono");
    dto.email = (String) row.get("email");
    dto.attiva = (Integer) row.get("attiva");
    return dto;
  }
}
