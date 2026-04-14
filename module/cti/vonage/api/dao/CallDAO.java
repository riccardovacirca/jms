package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la persistenza delle chiamate Vonage.
 * Accede alla tabella jms_cti_chiamate tramite il wrapper JDBC {@link DB}.
 */
public class CallDAO
{
  private final DB db;

  /**
   * @param db connessione DB iniettata dall'handler
   */
  public CallDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Inserisce una nuova chiamata. Restituisce l'id generato.
   *
   * @param dto dati della chiamata da persistere
   * @return id del record inserito
   */
  public Long insert(CallDTO dto) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_chiamate "
        + "(uuid, conversazione_uuid, conversation_name, direzione, stato, "
        + "tipo_mittente, numero_mittente, tipo_destinatario, numero_destinatario, "
        + "answer_url, event_url, operatore_id, chiamante_account_id, contatto_id, callback_url, data_creazione) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) "
        + "RETURNING id";
    rows = db.select(sql,
        dto.uuid(), dto.conversazioneUuid(), dto.conversationName(), dto.direzione(), dto.stato(),
        dto.tipoMittente(), dto.numeroMittente(), dto.tipoDestinatario(), dto.numeroDestinatario(),
        dto.answerUrl(), dto.eventUrl(), dto.operatoreId(), dto.chiamanteAccountId(),
        dto.contattoId(), dto.callbackUrl());
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Restituisce la lista paginata di chiamate, ordinate per data decrescente.
   *
   * @param page numero di pagina (da 1)
   * @param size elementi per pagina
   */
  public List<CallDTO> findAll(int page, int size) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    List<CallDTO> result;

    sql = "SELECT * FROM jms_cti_chiamate ORDER BY data_creazione DESC LIMIT ? OFFSET ?";
    rows = db.select(sql, size, (page - 1) * size);
    result = new ArrayList<>();
    for (HashMap<String, Object> r : rows) {
      result.add(toDTO(r));
    }
    return result;
  }

  /**
   * Conta il totale delle chiamate per la paginazione.
   *
   * @return numero totale di record
   */
  public int count() throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_cti_chiamate";
    rows = db.select(sql);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /**
   * Aggiorna lo stato di una chiamata identificata dal suo uuid Vonage.
   *
   * @param uuid  UUID Vonage della chiamata
   * @param stato nuovo stato (es. answered, completed)
   */
  public void updateStatus(String uuid, String stato) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_chiamate SET stato = ?, data_aggiornamento = NOW() WHERE uuid = ?";
    db.query(sql, stato, uuid);
  }

  /**
   * Aggiorna la chiamata quando il cliente risponde (evento {@code answered}).
   * Imposta {@code ora_inizio} e {@code stato = answered}.
   *
   * @param uuid       UUID Vonage della chiamata
   * @param oraInizio  timestamp della risposta
   */
  public void updateOnAnswer(String uuid, java.time.LocalDateTime oraInizio) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_chiamate SET stato = 'answered', ora_inizio = ?, "
        + "data_aggiornamento = NOW() WHERE uuid = ?";
    db.query(sql, oraInizio, uuid);
  }

  /**
   * Aggiorna la chiamata al completamento con tutti i dati di billing.
   * {@code ora_inizio} viene impostata solo se ancora null (caso in cui l'evento
   * {@code answered} non fosse stato ricevuto).
   *
   * @param uuid      UUID Vonage della chiamata
   * @param oraInizio timestamp inizio (da {@code start_time} dell'evento)
   * @param oraFine   timestamp fine (da {@code end_time} dell'evento)
   * @param durata    durata in secondi
   * @param tariffa   tariffa al minuto (da Vonage)
   * @param costo     costo totale (da Vonage)
   * @param rete      rete telefonica del destinatario
   */
  public void updateOnComplete(String uuid,
                                java.time.LocalDateTime oraInizio,
                                java.time.LocalDateTime oraFine,
                                Integer durata,
                                String tariffa,
                                String costo,
                                String rete) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_chiamate SET stato = 'completed', "
        + "ora_inizio = COALESCE(ora_inizio, ?), ora_fine = ?, "
        + "durata = ?, tariffa = ?, costo = ?, rete = ?, "
        + "data_aggiornamento = NOW() WHERE uuid = ?";
    db.query(sql, oraInizio, oraFine, durata, tariffa, costo, rete, uuid);
  }

  /**
   * Restituisce la lista paginata di chiamate con il nome dell'operatore,
   * ottimizzata per la risposta API (LEFT JOIN su {@code jms_cti_operatori}).
   *
   * @param page numero di pagina (da 1)
   * @param size elementi per pagina
   * @return lista di righe grezze pronte per la serializzazione JSON
   */
  /**
   * Lista paginata di tutte le chiamate (per ADMIN/ROOT), con nome operatore.
   *
   * @param page numero di pagina (da 1)
   * @param size elementi per pagina
   */
  public List<HashMap<String, Object>> findAllForApi(int page, int size) throws Exception
  {
    String sql;

    sql = "SELECT c.id, c.uuid, c.stato, c.numero_mittente, c.numero_destinatario, "
        + "c.durata, c.tariffa, c.costo, c.ora_inizio, c.ora_fine, "
        + "c.data_creazione, c.errore_titolo, o.nome AS operatore_nome "
        + "FROM jms_cti_chiamate c "
        + "LEFT JOIN jms_cti_operatori o ON o.id = c.operatore_id "
        + "ORDER BY c.data_creazione DESC LIMIT ? OFFSET ?";
    return db.select(sql, size, (page - 1) * size);
  }

  /**
   * Lista paginata delle sole chiamate dell'account indicato (per USER).
   *
   * @param page      numero di pagina (da 1)
   * @param size      elementi per pagina
   * @param accountId account corrente ({@code session.sub()})
   */
  public List<HashMap<String, Object>> findByAccountForApi(int page, int size, long accountId)
      throws Exception
  {
    String sql;

    sql = "SELECT c.id, c.uuid, c.stato, c.numero_mittente, c.numero_destinatario, "
        + "c.durata, c.tariffa, c.costo, c.ora_inizio, c.ora_fine, "
        + "c.data_creazione, c.errore_titolo, o.nome AS operatore_nome "
        + "FROM jms_cti_chiamate c "
        + "LEFT JOIN jms_cti_operatori o ON o.id = c.operatore_id "
        + "WHERE c.chiamante_account_id = ? "
        + "ORDER BY c.data_creazione DESC LIMIT ? OFFSET ?";
    return db.select(sql, accountId, size, (page - 1) * size);
  }

  /**
   * Conta le chiamate dell'account indicato (per paginazione USER).
   *
   * @param accountId account corrente
   */
  public int countByAccount(long accountId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT COUNT(*) AS n FROM jms_cti_chiamate WHERE chiamante_account_id = ?";
    rows = db.select(sql, accountId);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /**
   * Restituisce la chiamata identificata dall'uuid Vonage, o {@code null} se non trovata.
   *
   * @param uuid UUID Vonage della chiamata
   * @return DTO della chiamata, o null
   */
  public CallDTO findByUuid(String uuid) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_cti_chiamate WHERE uuid = ?";
    rows = db.select(sql, uuid);
    if (rows.isEmpty()) {
      return null;
    }
    return toDTO(rows.get(0));
  }

  /**
   * Restituisce la prima chiamata con stato {@code ringing} o {@code answered}
   * associata all'operatore indicato, o {@code null} se non ne esiste alcuna.
   * Usato dal frontend al reconnect per ripristinare lo stato in caso di page reload
   * avvenuto durante una conversazione.
   *
   * @param operatoreId id dell'operatore in jms_cti_operatori
   * @return riga grezza (uuid, stato, numero_destinatario) o null
   */
  public HashMap<String, Object> findActiveByOperatore(long operatoreId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT uuid, stato, numero_destinatario, conversation_name FROM jms_cti_chiamate "
        + "WHERE operatore_id = ? AND stato IN ('ringing', 'answered') "
        + "ORDER BY data_creazione DESC LIMIT 1";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty()) {
      return null;
    }
    return rows.get(0);
  }

  /**
   * Restituisce la chiamata identificata dal suo id, o {@code null} se non trovata.
   *
   * @param id id della chiamata in {@code jms_cti_chiamate}
   * @return DTO della chiamata, o null
   */
  public CallDTO findById(long id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql = "SELECT * FROM jms_cti_chiamate WHERE id = ?";
    rows = db.select(sql, id);
    if (rows.isEmpty()) {
      return null;
    }
    return toDTO(rows.get(0));
  }

  /**
   * Aggiorna {@code recording_url} e {@code recording_uuid} quando Vonage notifica
   * il completamento della registrazione tramite evento {@code recording}.
   *
   * @param uuid          UUID Vonage della chiamata
   * @param recordingUrl  URL di download della registrazione su Vonage
   * @param recordingUuid UUID della registrazione su Vonage
   */
  public void updateRecording(String uuid, String recordingUrl, String recordingUuid)
      throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_chiamate SET recording_url = ?, recording_uuid = ?, "
        + "data_aggiornamento = NOW() WHERE uuid = ?";
    db.query(sql, recordingUrl, recordingUuid, uuid);
  }

  /**
   * Aggiorna {@code recording_path} dopo che il file audio è stato scaricato e salvato
   * in storage locale.
   *
   * @param id   id della chiamata in {@code jms_cti_chiamate}
   * @param path percorso assoluto del file salvato
   */
  public void updateRecordingPath(long id, String path) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_chiamate SET recording_path = ?, data_aggiornamento = NOW() WHERE id = ?";
    db.query(sql, path, id);
  }

  /** Mappa un record del ResultSet nel DTO corrispondente. */
  private CallDTO toDTO(HashMap<String, Object> r)
  {
    return new CallDTO(
        DB.toLong(r.get("id")),
        DB.toString(r.get("uuid")),
        DB.toString(r.get("conversazione_uuid")),
        DB.toString(r.get("conversation_name")),
        DB.toString(r.get("direzione")),
        DB.toString(r.get("stato")),
        DB.toString(r.get("tipo_mittente")),
        DB.toString(r.get("numero_mittente")),
        DB.toString(r.get("tipo_destinatario")),
        DB.toString(r.get("numero_destinatario")),
        DB.toString(r.get("tariffa")),
        DB.toString(r.get("costo")),
        DB.toInteger(r.get("durata")),
        DB.toLocalDateTime(r.get("ora_inizio")),
        DB.toLocalDateTime(r.get("ora_fine")),
        DB.toString(r.get("rete")),
        DB.toString(r.get("answer_url")),
        DB.toString(r.get("event_url")),
        DB.toString(r.get("errore_titolo")),
        DB.toString(r.get("errore_dettaglio")),
        DB.toLong(r.get("operatore_id")),
        DB.toLong(r.get("chiamante_account_id")),
        DB.toLong(r.get("contatto_id")),
        DB.toString(r.get("callback_url")),
        DB.toString(r.get("recording_url")),
        DB.toString(r.get("recording_uuid")),
        DB.toString(r.get("recording_path")),
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLocalDateTime(r.get("data_aggiornamento")));
  }
}
