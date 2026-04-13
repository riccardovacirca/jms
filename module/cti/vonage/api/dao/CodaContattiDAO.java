package dev.jms.app.module.cti.vonage.dao;

import dev.jms.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la coda globale contatti CTI ({@code jms_cti_coda_contatti}).
 *
 * <p>Gestisce i contatti in ingresso condivisi tra tutti gli operatori.
 * L'estrazione dalla coda globale è delegata a {@link OperatoreContattiDAO#getNext}.</p>
 */
public class CodaContattiDAO
{
  private final DB db;

  public CodaContattiDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Inserisce un contatto nella coda globale.
   *
   * @param contattoJson JSON serializzato del contatto
   * @return id del record inserito
   */
  public long insert(String contattoJson) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_coda_contatti (contatto_json) VALUES (?::jsonb) RETURNING id";
    rows = db.select(sql, contattoJson);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Inserisce multipli contatti nella coda globale in modo massivo.
   *
   * @param contatti lista di JSON serializzati
   * @return numero di record inseriti
   */
  public int insertBulk(List<String> contatti) throws Exception
  {
    String sql;
    String values;
    List<Object> params;

    if (contatti.isEmpty()) {
      return 0;
    }

    values = "";
    params = new ArrayList<>();

    for (String contattoJson : contatti) {
      if (!values.isEmpty()) {
        values += ", ";
      }
      values += "(?::jsonb)";
      params.add(contattoJson);
    }

    sql = "INSERT INTO jms_cti_coda_contatti (contatto_json) VALUES " + values;
    return db.query(sql, params.toArray());
  }

  /**
   * Restituisce il numero di contatti in attesa nella coda globale.
   *
   * @return conteggio contatti in coda
   */
  public int count() throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS cnt FROM jms_cti_coda_contatti";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("cnt"));
  }
}
