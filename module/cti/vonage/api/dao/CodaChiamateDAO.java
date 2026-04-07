package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.CodaChiamateDTO;
import dev.jms.util.DB;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO per la coda chiamate CTI ({@code jms_cti_coda_chiamate}).
 *
 * <p>Gestisce la coda condivisa tra operatori: inserimento singolo/massivo,
 * estrazione del prossimo contatto, statistiche.</p>
 */
public class CodaChiamateDAO
{
  private final DB db;

  public CodaChiamateDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Inserisce un contatto nella coda.
   *
   * @param contattoJson JSON serializzato del contatto
   * @param priorita     priorità (default 0, numeri più alti = priorità maggiore)
   * @return id del record inserito
   */
  public long insert(String contattoJson, int priorita) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_coda_chiamate (contatto_json, priorita) "
        + "VALUES (?::jsonb, ?) RETURNING id";
    rows = db.select(sql, contattoJson, priorita);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Inserisce multipli contatti nella coda in modo massivo.
   *
   * @param contatti lista di JSON serializzati
   * @param priorita priorità comune per tutti
   * @return numero di record inseriti
   */
  public int insertBulk(List<String> contatti, int priorita) throws Exception
  {
    String sql;
    String values;
    List<Object> params;
    int idx;

    if (contatti.isEmpty()) {
      return 0;
    }

    values = "";
    params = new ArrayList<>();
    idx = 1;

    for (String contattoJson : contatti) {
      if (!values.isEmpty()) {
        values += ", ";
      }
      values += "(?::jsonb, ?)";
      params.add(contattoJson);
      params.add(priorita);
      idx += 2;
    }

    sql = "INSERT INTO jms_cti_coda_chiamate (contatto_json, priorita) VALUES " + values;
    return db.query(sql, params.toArray());
  }

  /**
   * Estrae il prossimo contatto dalla coda (stato pending) e lo assegna all'operatore.
   * Ordinamento: priorità DESC, data_inserimento ASC.
   *
   * @param operatoreId id operatore che prende in carico il contatto
   * @return contatto assegnato o {@code null} se la coda è vuota
   */
  public CodaChiamateDTO getNext(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    long id;

    db.begin();
    try {
      sql = "SELECT id FROM jms_cti_coda_chiamate "
          + "WHERE stato = 'pending' "
          + "ORDER BY priorita DESC, data_inserimento ASC "
          + "LIMIT 1 FOR UPDATE SKIP LOCKED";
      rows = db.select(sql);

      if (rows.isEmpty()) {
        db.commit();
        return null;
      }

      id = DB.toLong(rows.get(0).get("id"));

      sql = "UPDATE jms_cti_coda_chiamate "
          + "SET stato = 'assigned', operatore_id = ?, data_assegnazione = NOW() "
          + "WHERE id = ? RETURNING *";
      rows = db.select(sql, operatoreId, id);
      db.commit();

      return mapRow(rows.get(0));

    } catch (Exception e) {
      db.rollback();
      throw e;
    }
  }

  /**
   * Aggiorna lo stato e l'esito di un contatto nella coda.
   *
   * @param id    id del contatto
   * @param stato nuovo stato
   * @param esito esito opzionale
   */
  public void updateStato(long id, String stato, String esito) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_coda_chiamate "
        + "SET stato = ?, esito = ?, data_completamento = CASE WHEN ? IN ('completed', 'failed', 'cancelled') THEN NOW() ELSE data_completamento END "
        + "WHERE id = ?";
    db.query(sql, stato, esito, stato, id);
  }

  /**
   * Restituisce statistiche sulla coda.
   *
   * @param operatoreId id operatore (per statistiche personali), null per totali
   * @return mappa con conteggi per stato
   */
  public Map<String, Integer> getStats(Long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    Map<String, Integer> stats;

    stats = new HashMap<>();

    if (operatoreId == null) {
      sql = "SELECT stato, COUNT(*) as cnt FROM jms_cti_coda_chiamate GROUP BY stato";
      rows = db.select(sql);
    } else {
      sql = "SELECT stato, COUNT(*) as cnt FROM jms_cti_coda_chiamate WHERE operatore_id = ? GROUP BY stato";
      rows = db.select(sql, operatoreId);
    }

    for (HashMap<String, Object> row : rows) {
      String stato;
      int cnt;

      stato = (String) row.get("stato");
      cnt = DB.toInteger(row.get("cnt"));
      stats.put(stato, cnt);
    }

    return stats;
  }

  /**
   * Mappa una riga DB al DTO.
   */
  private CodaChiamateDTO mapRow(HashMap<String, Object> row)
  {
    return new CodaChiamateDTO(
      DB.toLong(row.get("id")),
      (String) row.get("contatto_json"),
      (String) row.get("stato"),
      DB.toInteger(row.get("priorita")),
      DB.toLong(row.get("operatore_id")),
      DB.toLocalDateTime(row.get("data_inserimento")),
      DB.toLocalDateTime(row.get("data_assegnazione")),
      DB.toLocalDateTime(row.get("data_completamento")),
      (String) row.get("esito"),
      (String) row.get("note")
    );
  }
}
