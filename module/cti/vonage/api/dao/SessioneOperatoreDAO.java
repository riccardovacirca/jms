package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.SessioneOperatoreDTO;
import dev.jms.util.DB;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la tabella {@code jms_sessione_operatore}.
 *
 * <p>Gestisce il ciclo di vita dei turni operatore: creazione da parte dell'admin,
 * aggiornamento alla connessione/disconnessione dell'operatore e alle chiamate.</p>
 *
 * <p>Valori di {@code stato}: 0=disconnesso, 1=connesso, 2=in pausa, 3=in chiamata.</p>
 */
public class SessioneOperatoreDAO
{
  private final DB db;

  /**
   * @param db connessione DB iniettata dall'handler
   */
  public SessioneOperatoreDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Inserisce un nuovo turno pianificato. Stato iniziale: 0 (disconnesso).
   *
   * @param operatoreId id operatore
   * @param turnoInizio inizio turno pianificato
   * @param turnoFine   fine turno pianificato
   * @param note        note opzionali
   * @param creatoDA    account_id dell'admin che crea il turno
   * @return id del record inserito
   */
  public long insert(long operatoreId, LocalDateTime turnoInizio, LocalDateTime turnoFine,
                     String note, long creatoDA) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_sessione_operatore "
        + "(operatore_id, turno_inizio, turno_fine, note, creato_da) "
        + "VALUES (?, ?, ?, ?, ?) RETURNING id";
    rows = db.select(sql, operatoreId, turnoInizio, turnoFine, note, creatoDA);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Aggiorna turno_inizio, turno_fine e note di un turno esistente (solo admin).
   *
   * @param id          id del turno
   * @param turnoInizio nuovo inizio turno
   * @param turnoFine   nuova fine turno
   * @param note        nuove note
   * @param modificatoDA account_id dell'admin che modifica
   */
  public void update(long id, LocalDateTime turnoInizio, LocalDateTime turnoFine,
                     String note, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore "
        + "SET turno_inizio = ?, turno_fine = ?, note = ?, "
        + "modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, turnoInizio, turnoFine, note, modificatoDA, id);
  }

  /**
   * Elimina un turno (solo se stato = 0, non ancora avviato).
   *
   * @param id id del turno
   */
  public void delete(long id) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_sessione_operatore WHERE id = ? AND stato = 0";
    db.query(sql, id);
  }

  /**
   * Trova il turno attivo o pianificato per l'operatore nell'orario corrente.
   * Un turno è candidato se {@code turno_inizio <= NOW() <= turno_fine}
   * e lo stato è 0 (disconnesso) o 2 (in pausa).
   *
   * @param operatoreId id operatore
   * @return turno trovato o {@code null}
   */
  public SessioneOperatoreDTO findCorrente(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_sessione_operatore "
        + "WHERE operatore_id = ? AND turno_inizio <= NOW() AND turno_fine >= NOW() "
        + "AND stato IN (0, 2) ORDER BY turno_inizio DESC LIMIT 1";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Segna la connessione dell'operatore: imposta {@code connessione_inizio} (se prima connessione)
   * e porta lo stato a 1 (connesso). Aggiorna {@code data_modifica}.
   *
   * @param id          id del turno
   * @param modificatoDA account_id dell'operatore/sistema che aggiorna
   */
  public void registraConnessione(long id, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore "
        + "SET connessione_inizio = COALESCE(connessione_inizio, NOW()), "
        + "ultima_connessione = NOW(), "
        + "stato = 1, modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, modificatoDA, id);
  }

  /**
   * Segna la disconnessione dell'operatore durante il turno (pausa):
   * incrementa {@code numero_pause}, aggiunge la durata della pausa corrente
   * e porta lo stato a 2 (in pausa).
   *
   * @param id           id del turno
   * @param durataPausa  durata della pausa in secondi
   * @param modificatoDA account_id dell'operatore/sistema che aggiorna
   */
  public void registraPausa(long id, int durataPausa, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore "
        + "SET numero_pause = numero_pause + 1, "
        + "durata_pause = durata_pause + ?, "
        + "stato = 2, modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, durataPausa, modificatoDA, id);
  }

  /**
   * Chiude il turno: imposta {@code connessione_fine}, calcola {@code durata_totale}
   * e porta lo stato a 0 (disconnesso).
   *
   * @param id           id del turno
   * @param modificatoDA account_id dell'operatore/sistema che aggiorna
   */
  public void chiudiTurno(long id, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore "
        + "SET connessione_fine = NOW(), "
        + "durata_totale = EXTRACT(EPOCH FROM (NOW() - connessione_inizio))::INTEGER, "
        + "stato = 0, modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, modificatoDA, id);
  }

  /**
   * Aggiorna lo stato a 3 (in chiamata).
   *
   * @param operatoreId id operatore (cerca il turno attivo con stato 1)
   */
  public void setInChiamata(long operatoreId) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore SET stato = 3, data_modifica = NOW() "
        + "WHERE operatore_id = ? AND stato = 1 "
        + "AND turno_inizio <= NOW() AND turno_fine >= NOW()";
    db.query(sql, operatoreId);
  }

  /**
   * Riporta lo stato da 3 (in chiamata) a 1 (connesso) e aggiorna le statistiche chiamata.
   *
   * @param operatoreId        id operatore
   * @param durataChiamata     durata della chiamata in secondi
   */
  public void registraFineChiamata(long operatoreId, int durataChiamata) throws Exception
  {
    String sql;

    sql = "UPDATE jms_sessione_operatore "
        + "SET stato = 1, "
        + "numero_chiamate = numero_chiamate + 1, "
        + "durata_conversazione = durata_conversazione + ?, "
        + "data_modifica = NOW() "
        + "WHERE operatore_id = ? AND stato = 3 "
        + "AND turno_inizio <= NOW() AND turno_fine >= NOW()";
    db.query(sql, durataChiamata, operatoreId);
  }

  /**
   * Lista paginata dei turni, ordine decrescente per turno_inizio.
   *
   * @param page numero di pagina (da 1)
   * @param size elementi per pagina
   * @return lista di DTO
   */
  public List<SessioneOperatoreDTO> findAll(int page, int size) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<SessioneOperatoreDTO> result;

    sql = "SELECT * FROM jms_sessione_operatore ORDER BY turno_inizio DESC LIMIT ? OFFSET ?";
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

    sql = "SELECT COUNT(*) AS n FROM jms_sessione_operatore";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /**
   * Trova un turno per id.
   *
   * @param id chiave primaria
   * @return DTO o {@code null}
   */
  public SessioneOperatoreDTO findById(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_sessione_operatore WHERE id = ?";
    rows = db.select(sql, id);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /** Mappa una riga del ResultSet nel DTO. */
  private SessioneOperatoreDTO mapRow(HashMap<String, Object> r)
  {
    return new SessioneOperatoreDTO(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("operatore_id")),
        DB.toLocalDateTime(r.get("turno_inizio")),
        DB.toLocalDateTime(r.get("turno_fine")),
        DB.toLocalDateTime(r.get("connessione_inizio")),
        DB.toLocalDateTime(r.get("connessione_fine")),
        DB.toInteger(r.get("durata_totale")),
        DB.toInteger(r.get("numero_pause")),
        DB.toInteger(r.get("durata_pause")),
        DB.toLocalDateTime(r.get("ultima_connessione")),
        DB.toInteger(r.get("numero_chiamate")),
        DB.toInteger(r.get("durata_conversazione")),
        DB.toInteger(r.get("stato")),
        DB.toString(r.get("note")),
        DB.toLong(r.get("creato_da")),
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLong(r.get("modificato_da")),
        DB.toLocalDateTime(r.get("data_modifica"))
    );
  }
}
