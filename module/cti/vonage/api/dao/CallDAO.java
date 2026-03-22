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
        + "(uuid, conversation_uuid, direction, status, "
        + "from_type, from_number, to_type, to_number, "
        + "answer_url, event_url, operator_id, contatto_id, created_at) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) "
        + "RETURNING id";
    rows = db.select(sql,
        dto.uuid(), dto.conversationUuid(), dto.direction(), dto.status(),
        dto.fromType(), dto.fromNumber(), dto.toType(), dto.toNumber(),
        dto.answerUrl(), dto.eventUrl(), dto.operatorId(), dto.contattoId());
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

    sql = "SELECT * FROM chiamate ORDER BY created_at DESC LIMIT ? OFFSET ?";
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
   * @param uuid   UUID Vonage della chiamata
   * @param status nuovo stato (es. answered, completed)
   */
  public void updateStatus(String uuid, String status) throws Exception
  {
    String sql;

    sql = "UPDATE chiamate SET status = ?, updated_at = NOW() WHERE uuid = ?";
    db.query(sql, status, uuid);
  }

  /** Mappa un record del ResultSet nel DTO corrispondente. */
  private CallDTO toDTO(HashMap<String, Object> r)
  {
    return new CallDTO(
        DB.toLong(r.get("id")),
        DB.toString(r.get("uuid")),
        DB.toString(r.get("conversation_uuid")),
        DB.toString(r.get("direction")),
        DB.toString(r.get("status")),
        DB.toString(r.get("from_type")),
        DB.toString(r.get("from_number")),
        DB.toString(r.get("to_type")),
        DB.toString(r.get("to_number")),
        DB.toString(r.get("rate")),
        DB.toString(r.get("price")),
        DB.toInteger(r.get("duration")),
        DB.toLocalDateTime(r.get("start_time")),
        DB.toLocalDateTime(r.get("end_time")),
        DB.toString(r.get("network")),
        DB.toString(r.get("answer_url")),
        DB.toString(r.get("event_url")),
        DB.toString(r.get("error_title")),
        DB.toString(r.get("error_detail")),
        DB.toLong(r.get("operator_id")),
        DB.toLong(r.get("contatto_id")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));
  }
}
