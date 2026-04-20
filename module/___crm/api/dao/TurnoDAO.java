package dev.jms.app.crm.dao;

import dev.jms.app.crm.dto.TurnoDTO;
import dev.jms.util.DB;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la gestione dei turni pianificati ({@code jms_crm_turno}).
 */
public class TurnoDAO
{
  private final DB db;

  /**
   * @param db connessione DB iniettata dall'handler
   */
  public TurnoDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Inserisce un nuovo turno. Restituisce l'id generato.
   *
   * @param operatoreId id operatore in {@code jms_cti_operatori}
   * @param turnoInizio inizio turno
   * @param turnoFine   fine turno
   * @param note        note opzionali
   * @param creatoDA    account_id dell'admin che crea il turno
   * @return id del record inserito
   */
  public long insert(long operatoreId, LocalDateTime turnoInizio, LocalDateTime turnoFine,
                     String note, long creatoDA) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_crm_turno "
        + "(operatore_id, turno_inizio, turno_fine, note, creato_da) "
        + "VALUES (?, ?, ?, ?, ?) RETURNING id";
    rows = db.select(sql, operatoreId, turnoInizio, turnoFine, note, creatoDA);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Aggiorna un turno esistente.
   *
   * @param id           id del turno
   * @param turnoInizio  nuovo inizio turno
   * @param turnoFine    nuova fine turno
   * @param note         nuove note
   * @param modificatoDA account_id dell'admin che modifica
   */
  public void update(long id, LocalDateTime turnoInizio, LocalDateTime turnoFine,
                     String note, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_crm_turno "
        + "SET turno_inizio = ?, turno_fine = ?, note = ?, "
        + "modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, turnoInizio, turnoFine, note, modificatoDA, id);
  }

  /**
   * Elimina un turno.
   *
   * @param id id del turno
   */
  public void delete(long id) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_crm_turno WHERE id = ?";
    db.query(sql, id);
  }

  /**
   * Trova il turno attivo o pianificato per l'operatore nell'orario corrente.
   * Un turno è candidato se {@code turno_inizio <= NOW() <= turno_fine}.
   *
   * @param operatoreId id operatore in {@code jms_cti_operatori}
   * @return turno trovato o {@code null}
   */
  public TurnoDTO findCorrente(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_crm_turno "
        + "WHERE operatore_id = ? AND turno_inizio <= NOW() AND turno_fine >= NOW() "
        + "ORDER BY turno_inizio DESC LIMIT 1";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Lista paginata dei turni, ordinate per turno_inizio decrescente.
   *
   * @param page numero di pagina (da 1)
   * @param size elementi per pagina
   * @return lista di DTO
   */
  public List<TurnoDTO> findAll(int page, int size) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<TurnoDTO> result;

    sql = "SELECT * FROM jms_crm_turno ORDER BY turno_inizio DESC LIMIT ? OFFSET ?";
    rows = db.select(sql, size, (page - 1) * size);
    result = new ArrayList<>();
    for (HashMap<String, Object> r : rows) {
      result.add(mapRow(r));
    }
    return result;
  }

  /**
   * Conta il totale dei turni.
   */
  public int count() throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_crm_turno";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /**
   * Trova un turno per id.
   *
   * @param id chiave primaria
   * @return DTO o {@code null}
   */
  public TurnoDTO findById(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_crm_turno WHERE id = ?";
    rows = db.select(sql, id);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /** Mappa una riga del ResultSet nel DTO. */
  private TurnoDTO mapRow(HashMap<String, Object> r)
  {
    return new TurnoDTO(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toLocalDateTime(r.get("turno_inizio")),
        DB.toLocalDateTime(r.get("turno_fine")),
        DB.toString(r.get("note")),
        DB.toLong(r.get("creato_da")),
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLong(r.get("modificato_da")),
        DB.toLocalDateTime(r.get("data_modifica")));
  }
}
