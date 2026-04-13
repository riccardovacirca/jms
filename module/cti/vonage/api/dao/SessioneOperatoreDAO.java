package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.SessioneOperatoreDTO;
import dev.jms.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la tabella {@code jms_cti_sessione_operatore}.
 *
 * <p>Gestisce il ciclo di vita delle sessioni tecniche CTI: apertura automatica
 * alla connessione dell'operatore, aggiornamento agli eventi di connessione/pausa/chiamata.</p>
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
   * Apre una nuova sessione tecnica per l'operatore. Stato iniziale: 0 (disconnesso).
   * Chiamato automaticamente da {@code sdkToken} se non esiste già una sessione attiva.
   *
   * @param operatoreId id operatore in {@code jms_cti_operatori}
   * @param creatoDA    account_id dell'operatore che apre la sessione
   * @return id del record inserito
   */
  public long openSession(long operatoreId, long creatoDA) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_sessione_operatore (operatore_id, creato_da) "
        + "VALUES (?, ?) RETURNING id";
    rows = db.select(sql, operatoreId, creatoDA);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Trova la sessione tecnica attiva per l'operatore (stato 1=connesso, 2=in pausa, 3=in chiamata).
   * Restituisce la più recente in caso di sessioni multiple.
   *
   * @param operatoreId id operatore
   * @return sessione trovata o {@code null}
   */
  public SessioneOperatoreDTO findActive(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_cti_sessione_operatore "
        + "WHERE operatore_id = ? AND stato IN (1, 2, 3) "
        + "ORDER BY data_creazione DESC LIMIT 1";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Segna la connessione dell'operatore: imposta {@code connessione_inizio} (se prima connessione),
   * aggiorna {@code ultima_connessione} e porta lo stato a 1 (connesso).
   *
   * @param id           id della sessione
   * @param modificatoDA account_id dell'operatore/sistema che aggiorna
   */
  public void registraConnessione(long id, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_sessione_operatore "
        + "SET connessione_inizio = COALESCE(connessione_inizio, NOW()), "
        + "ultima_connessione = NOW(), "
        + "stato = 1, modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, modificatoDA, id);
  }

  /**
   * Segna la disconnessione dell'operatore (pausa): incrementa {@code numero_pause},
   * aggiunge la durata della pausa corrente e porta lo stato a 2 (in pausa).
   *
   * @param id           id della sessione
   * @param durataPausa  durata della pausa in secondi
   * @param modificatoDA account_id dell'operatore/sistema che aggiorna
   */
  public void registraPausa(long id, int durataPausa, long modificatoDA) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_sessione_operatore "
        + "SET numero_pause = numero_pause + 1, "
        + "durata_pause = durata_pause + ?, "
        + "stato = 2, modificato_da = ?, data_modifica = NOW() "
        + "WHERE id = ?";
    db.query(sql, durataPausa, modificatoDA, id);
  }

  /**
   * Aggiorna lo stato a 3 (in chiamata) per la sessione attiva dell'operatore.
   *
   * @param operatoreId id operatore (cerca la sessione con stato 1)
   */
  public void setInChiamata(long operatoreId) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_sessione_operatore SET stato = 3, data_modifica = NOW() "
        + "WHERE operatore_id = ? AND stato = 1 "
        + "ORDER BY data_creazione DESC LIMIT 1";
    db.query(sql, operatoreId);
  }

  /**
   * Riporta lo stato da 3 (in chiamata) a 1 (connesso) e aggiorna le statistiche chiamata.
   *
   * @param operatoreId    id operatore
   * @param durataChiamata durata della chiamata in secondi
   */
  public void registraFineChiamata(long operatoreId, int durataChiamata) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_sessione_operatore "
        + "SET stato = 1, "
        + "numero_chiamate = numero_chiamate + 1, "
        + "durata_conversazione = durata_conversazione + ?, "
        + "data_modifica = NOW() "
        + "WHERE operatore_id = ? AND stato = 3 "
        + "ORDER BY data_creazione DESC LIMIT 1";
    db.query(sql, durataChiamata, operatoreId);
  }

  /**
   * Lista paginata delle sessioni, ordinate per data di creazione decrescente.
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

    sql = "SELECT * FROM jms_cti_sessione_operatore ORDER BY data_creazione DESC LIMIT ? OFFSET ?";
    rows = db.select(sql, size, (page - 1) * size);
    result = new ArrayList<>();
    for (HashMap<String, Object> r : rows) {
      result.add(mapRow(r));
    }
    return result;
  }

  /**
   * Conta il totale delle sessioni.
   */
  public int count() throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_cti_sessione_operatore";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /**
   * Trova una sessione per id.
   *
   * @param id chiave primaria
   * @return DTO o {@code null}
   */
  public SessioneOperatoreDTO findById(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_cti_sessione_operatore WHERE id = ?";
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
        DB.toLocalDateTime(r.get("connessione_inizio")),
        DB.toLocalDateTime(r.get("connessione_fine")),
        DB.toInteger(r.get("durata_totale")),
        DB.toInteger(r.get("numero_pause")),
        DB.toInteger(r.get("durata_pause")),
        DB.toLocalDateTime(r.get("ultima_connessione")),
        DB.toInteger(r.get("numero_chiamate")),
        DB.toInteger(r.get("durata_conversazione")),
        DB.toInteger(r.get("stato")),
        DB.toLong(r.get("creato_da")),
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLong(r.get("modificato_da")),
        DB.toLocalDateTime(r.get("data_modifica")));
  }
}
