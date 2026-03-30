package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.CallDTO;
import dev.jms.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la persistenza delle chiamate Vonage.
 * Accede alla tabella chiamate tramite il wrapper JDBC {@link DB}.
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

    sql = "INSERT INTO chiamate "
        + "(uuid, conversazione_uuid, direzione, stato, "
        + "tipo_mittente, numero_mittente, tipo_destinatario, numero_destinatario, "
        + "answer_url, event_url, operatore_id, contatto_id, data_creazione) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) "
        + "RETURNING id";
    rows = db.select(sql,
        dto.uuid(), dto.conversazioneUuid(), dto.direzione(), dto.stato(),
        dto.tipoMittente(), dto.numeroMittente(), dto.tipoDestinatario(), dto.numeroDestinatario(),
        dto.answerUrl(), dto.eventUrl(), dto.operatoreId(), dto.contattoId());
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

    sql = "SELECT * FROM chiamate ORDER BY data_creazione DESC LIMIT ? OFFSET ?";
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

    sql = "SELECT COUNT(*) AS n FROM chiamate";
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

    sql = "UPDATE chiamate SET stato = ?, data_aggiornamento = NOW() WHERE uuid = ?";
    db.query(sql, stato, uuid);
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
        DB.toLong(r.get("contatto_id")),
        DB.toLocalDateTime(r.get("data_creazione")),
        DB.toLocalDateTime(r.get("data_aggiornamento")));
  }
}
