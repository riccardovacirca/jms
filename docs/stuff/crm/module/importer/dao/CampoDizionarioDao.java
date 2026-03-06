package dev.crm.module.importer.dao;

import dev.crm.module.importer.dto.CampoDizionarioDto;
import dev.springtools.util.DB;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class CampoDizionarioDao
{
  private final DataSource dataSource;

  public CampoDizionarioDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /** Trova tutti i campi ordinati per ordine */
  public List<CampoDizionarioDto> findAll() throws Exception
  {
    DB db;
    List<CampoDizionarioDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM contatti_campo_dizionario ORDER BY ordine";
      rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /** Trova un campo per nome */
  public Optional<CampoDizionarioDto>
  findByNomeCampo(String nomeCampo) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<CampoDizionarioDto> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT * FROM contatti_campo_dizionario WHERE nome_campo = ?";
      rs = db.select(sql, nomeCampo);
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

  /** Trova tutti i campi obbligatori */
  public List<CampoDizionarioDto> findObbligatori() throws Exception
  {
    DB db;
    List<CampoDizionarioDto> result;
    String sql;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM contatti_campo_dizionario " +
            "WHERE obbligatorio = 1 ORDER BY ordine";
      rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  /** Trova l'etichetta di un campo dato il nome */
  public String getEtichetta(String nomeCampo) throws Exception
  {
    Optional<CampoDizionarioDto> campo;
    String result;

    campo = findByNomeCampo(nomeCampo);
    result = campo.map(c -> c.etichetta).orElse(nomeCampo);
    return result;
  }

  private CampoDizionarioDto mapRecord(HashMap<String, Object> r)
  {
    CampoDizionarioDto result;

    result = new CampoDizionarioDto(
      DB.toLong(r.get("id")),
      DB.toString(r.get("nome_campo")),
      DB.toString(r.get("etichetta")),
      DB.toString(r.get("descrizione")),
      DB.toString(r.get("tipo_dato")),
      DB.toBoolean(r.get("obbligatorio")),
      DB.toInteger(r.get("ordine")),
      DB.toLocalDateTime(r.get("created_at")),
      DB.toLocalDateTime(r.get("updated_at")));
    return result;
  }
}
