package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la persistenza delle chiamate Vonage.
 * Accede alla tabella jms_chiamate tramite il wrapper JDBC {@link DB}.
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

    sql = "INSERT INTO jms_chiamate "
        + "(uuid, conversazione_uuid, direzione, stato, "
        + "tipo_mittente, numero_mittente, tipo_destinatario, numero_destinatario, "
        + "answer_url, event_url, operatore_id, chiamante_account_id, contatto_id, data_creazione) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) "
        + "RETURNING id";
    rows = db.select(sql,
        dto.uuid(), dto.conversazioneUuid(), dto.direzione(), dto.stato(),
        dto.tipoMittente(), dto.numeroMittente(), dto.tipoDestinatario(), dto.numeroDestinatario(),
        dto.answerUrl(), dto.eventUrl(), dto.operatoreId(), dto.chiamanteAccountId(), dto.contattoId());
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

    sql = "SELECT * FROM jms_chiamate ORDER BY data_creazione DESC LIMIT ? OFFSET ?";
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

    sql = "SELECT COUNT(*) AS n FROM jms_chiamate";
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

    sql = "UPDATE jms_chiamate SET stato = ?, data_aggiornamento = NOW() WHERE uuid = ?";
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

    sql = "UPDATE jms_chiamate SET stato = 'answered', ora_inizio = ?, "
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

    sql = "UPDATE jms_chiamate SET stato = 'completed', "
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
        + "FROM jms_chiamate c "
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
        + "FROM jms_chiamate c "
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

    sql = "SELECT COUNT(*) AS n FROM jms_chiamate WHERE chiamante_account_id = ?";
    rows = db.select(sql, accountId);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Mappa un record del ResultSet nel DTO corrispondente. */
  private CallDTO toDTO(HashMap<String, Object> r)
  {
    return new CallDTO(
        DB.toLong(r.get("id")),
        DB.toString(r.get("uuid")),
        DB.toString(r.get("conversazione_uuid")),
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
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLocalDateTime(r.get("data_aggiornamento")));
  }
}
