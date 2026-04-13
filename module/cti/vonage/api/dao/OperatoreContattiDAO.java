package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.OperatoreContattoDTO;
import dev.jms.util.DB;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la coda personale di un operatore CTI ({@code jms_cti_operatore_contatti}).
 *
 * <p>Gestisce i contatti assegnati a un operatore specifico, inclusa la pianificazione
 * di richiamate future. L'estrazione dalla coda globale avviene in modo atomico
 * all'interno di {@link #getNext}.</p>
 */
public class OperatoreContattiDAO
{
  private final DB db;

  public OperatoreContattiDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Restituisce il prossimo contatto disponibile per l'operatore.
   *
   * <p>Prima cerca nella coda personale dell'operatore i contatti con
   * {@code pianificato_al <= NOW()} ordinati per {@code data_inserimento ASC} (FIFO).
   * Se la coda personale è vuota, estrae il prossimo contatto dalla coda globale
   * ({@code jms_cti_coda_contatti}), lo inserisce nella coda personale e lo restituisce.
   * Se anche la coda globale è vuota, restituisce {@code null}.</p>
   *
   * @param operatoreId id dell'operatore
   * @return prossimo contatto disponibile o {@code null} se non ci sono contatti
   */
  public OperatoreContattoDTO getNext(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    long codaGlobaleId;
    String contattoJson;
    long nuovoId;

    sql = "SELECT * FROM jms_cti_operatore_contatti "
        + "WHERE operatore_id = ? AND pianificato_al <= NOW() "
        + "ORDER BY data_inserimento ASC LIMIT 1";
    rows = db.select(sql, operatoreId);

    if (!rows.isEmpty()) {
      return mapRow(rows.get(0));
    }

    db.begin();
    try {
      sql = "SELECT id, contatto_json FROM jms_cti_coda_contatti "
          + "ORDER BY data_inserimento ASC LIMIT 1 FOR UPDATE SKIP LOCKED";
      rows = db.select(sql);

      if (rows.isEmpty()) {
        db.commit();
        return null;
      }

      codaGlobaleId = DB.toLong(rows.get(0).get("id"));
      contattoJson  = DB.toString(rows.get(0).get("contatto_json"));

      sql = "INSERT INTO jms_cti_operatore_contatti (operatore_id, contatto_json) "
          + "VALUES (?, ?::jsonb) RETURNING id";
      rows = db.select(sql, operatoreId, contattoJson);
      nuovoId = DB.toLong(rows.get(0).get("id"));

      sql = "DELETE FROM jms_cti_coda_contatti WHERE id = ?";
      db.query(sql, codaGlobaleId);

      db.commit();

    } catch (Exception e) {
      try { db.rollback(); } catch (Exception ignored) {}
      throw e;
    }

    sql = "SELECT * FROM jms_cti_operatore_contatti WHERE id = ?";
    rows = db.select(sql, nuovoId);
    return mapRow(rows.get(0));
  }

  /**
   * Restituisce il primo contatto disponibile nella coda personale dell'operatore.
   * Usato al reconnect del frontend per ripristinare lo stato senza riestrarre dalla coda globale.
   *
   * @param operatoreId id dell'operatore
   * @return primo contatto disponibile o {@code null}
   */
  public OperatoreContattoDTO getFirstAvailable(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_cti_operatore_contatti "
        + "WHERE operatore_id = ? AND pianificato_al <= NOW() "
        + "ORDER BY data_inserimento ASC LIMIT 1";
    rows = db.select(sql, operatoreId);

    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Pianifica un contatto per un richiamo futuro aggiornando {@code pianificato_al}.
   *
   * @param id            id del record in {@code jms_cti_operatore_contatti}
   * @param operatoreId   id dell'operatore proprietario del record
   * @param pianificatoAl data/ora dalla quale il contatto torna disponibile
   */
  public void pianifica(long id, long operatoreId, LocalDateTime pianificatoAl) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatore_contatti SET pianificato_al = ? WHERE id = ? AND operatore_id = ?";
    db.query(sql, pianificatoAl, id, operatoreId);
  }

  /**
   * Rimuove un contatto dalla coda personale dell'operatore (contatto chiamato).
   *
   * @param id          id del record
   * @param operatoreId id dell'operatore proprietario del record
   */
  public void rimuovi(long id, long operatoreId) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_cti_operatore_contatti WHERE id = ? AND operatore_id = ?";
    db.query(sql, id, operatoreId);
  }

  /**
   * Restituisce il conteggio dei contatti nella coda personale dell'operatore,
   * suddivisi tra disponibili e pianificati.
   *
   * @param operatoreId id operatore o {@code null} per tutti gli operatori
   * @return mappa con chiavi {@code disponibili} e {@code pianificati}
   */
  public java.util.Map<String, Integer> countByStato(Long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    java.util.Map<String, Integer> result;

    result = new java.util.HashMap<>();

    if (operatoreId == null) {
      sql = "SELECT COUNT(*) AS cnt FROM jms_cti_operatore_contatti WHERE pianificato_al <= NOW()";
      rows = db.select(sql);
      result.put("disponibili", DB.toInteger(rows.get(0).get("cnt")));

      sql = "SELECT COUNT(*) AS cnt FROM jms_cti_operatore_contatti WHERE pianificato_al > NOW()";
      rows = db.select(sql);
      result.put("pianificati", DB.toInteger(rows.get(0).get("cnt")));
    } else {
      sql = "SELECT COUNT(*) AS cnt FROM jms_cti_operatore_contatti WHERE operatore_id = ? AND pianificato_al <= NOW()";
      rows = db.select(sql, operatoreId);
      result.put("disponibili", DB.toInteger(rows.get(0).get("cnt")));

      sql = "SELECT COUNT(*) AS cnt FROM jms_cti_operatore_contatti WHERE operatore_id = ? AND pianificato_al > NOW()";
      rows = db.select(sql, operatoreId);
      result.put("pianificati", DB.toInteger(rows.get(0).get("cnt")));
    }

    return result;
  }

  /**
   * Restituisce tutti i contatti nella coda personale dell'operatore, indipendentemente
   * da {@code pianificato_al}. Usato dalla dashboard admin per il monitoraggio degli orfani.
   *
   * @param operatoreId id dell'operatore
   * @return lista di tutti i contatti nella coda, ordinata per {@code data_inserimento ASC}
   */
  public List<OperatoreContattoDTO> findByOperatore(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<OperatoreContattoDTO> result;

    sql = "SELECT * FROM jms_cti_operatore_contatti "
        + "WHERE operatore_id = ? ORDER BY data_inserimento ASC";
    rows   = db.select(sql, operatoreId);
    result = new java.util.ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }
    return result;
  }

  /**
   * Restituisce tutti i contatti orfani nella coda personale dell'operatore.
   *
   * <p>Un orfano è un contatto il cui {@code pianificato_al <= NOW()}, ovvero disponibile
   * ma non ancora chiamato né pianificato esplicitamente per il futuro.</p>
   *
   * @param operatoreId id dell'operatore
   * @return lista dei contatti orfani ordinati per {@code data_inserimento ASC}
   */
  public List<OperatoreContattoDTO> findOrphans(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<OperatoreContattoDTO> result;

    sql = "SELECT * FROM jms_cti_operatore_contatti "
        + "WHERE operatore_id = ? AND pianificato_al <= NOW() "
        + "ORDER BY data_inserimento ASC";
    rows   = db.select(sql, operatoreId);
    result = new java.util.ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }
    return result;
  }

  /**
   * Rimuove un contatto dalla coda personale tramite id, senza verifica dell'operatore.
   * Usato dalla dashboard admin per il cleanup forzato degli orfani.
   *
   * @param id id del record in {@code jms_cti_operatore_contatti}
   */
  public void rimuoviById(long id) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_cti_operatore_contatti WHERE id = ?";
    db.query(sql, id);
  }

  /**
   * Mappa una riga DB al DTO.
   */
  private OperatoreContattoDTO mapRow(HashMap<String, Object> row)
  {
    return new OperatoreContattoDTO(
      DB.toLong(row.get("id")),
      DB.toLong(row.get("operatore_id")),
      DB.toString(row.get("contatto_json")),
      DB.toLocalDateTime(row.get("data_inserimento")),
      DB.toLocalDateTime(row.get("pianificato_al"))
    );
  }
}
