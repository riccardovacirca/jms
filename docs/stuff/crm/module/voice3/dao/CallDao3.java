package dev.crm.module.voice3.dao;

import dev.crm.module.voice3.dto.CallDto3;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * DAO per la persistenza delle chiamate Vonage.
 * Accede direttamente alla tabella voice_calls tramite JDBC.
 *
 * La tabella voice_calls è condivisa con i moduli voice e voice2:
 * voice3 la utilizza in sola lettura/scrittura senza modificarne la struttura.
 * Le migration che definiscono la tabella si trovano in:
 *   - V20260203_120002__module_voice.sql
 *   - V20260210_133244__add_operator_to_voice_calls.sql
 */
public class CallDao3
{
  private final DataSource dataSource;

  public CallDao3(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  /**
   * Inserisce una nuova chiamata nel database.
   * Viene chiamato subito dopo che Vonage ha confermato la creazione della chiamata
   * (risposta al POST su /v1/calls), per tracciare uuid e conversationUuid assegnati.
   *
   * @return id del record inserito
   */
  public long insert(CallDto3 dto) throws Exception
  {
    DB db;
    String sql;
    long id;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "INSERT INTO voice_calls (uuid, conversation_uuid, direction, status, "
          + "from_type, from_number, to_type, to_number, rate, price, duration, "
          + "start_time, end_time, network, answer_url, event_url, "
          + "error_title, error_detail, operator_id, campagna_id, contatto_id, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.uuid,
          dto.conversationUuid,
          dto.direction,
          dto.status,
          dto.fromType,
          dto.fromNumber,
          dto.toType,
          dto.toNumber,
          dto.rate,
          dto.price,
          dto.duration,
          dto.startTime != null ? DB.toSqlTimestamp(dto.startTime) : null,
          dto.endTime != null ? DB.toSqlTimestamp(dto.endTime) : null,
          dto.network,
          dto.answerUrl,
          dto.eventUrl,
          dto.errorTitle,
          dto.errorDetail,
          dto.operatorId,
          dto.campagnaId,
          dto.contattoId,
          DB.toSqlTimestamp(LocalDateTime.now()));
      id = db.lastInsertId();

      return id;
    } finally {
      db.release();
    }
  }

  /**
   * Aggiorna tutti i campi di una chiamata esistente.
   * Usato per aggiornare i dettagli ricevuti negli eventi webhook successivi.
   */
  public int update(CallDto3 dto) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE voice_calls SET conversation_uuid = ?, status = ?, "
          + "from_type = ?, from_number = ?, to_type = ?, to_number = ?, "
          + "rate = ?, price = ?, duration = ?, start_time = ?, end_time = ?, "
          + "network = ?, answer_url = ?, event_url = ?, "
          + "error_title = ?, error_detail = ?, operator_id = ?, campagna_id = ?, "
          + "contatto_id = ?, updated_at = ? WHERE id = ?";
      result = db.query(
          sql,
          dto.conversationUuid,
          dto.status,
          dto.fromType,
          dto.fromNumber,
          dto.toType,
          dto.toNumber,
          dto.rate,
          dto.price,
          dto.duration,
          dto.startTime != null ? DB.toSqlTimestamp(dto.startTime) : null,
          dto.endTime != null ? DB.toSqlTimestamp(dto.endTime) : null,
          dto.network,
          dto.answerUrl,
          dto.eventUrl,
          dto.errorTitle,
          dto.errorDetail,
          dto.operatorId,
          dto.campagnaId,
          dto.contattoId,
          DB.toSqlTimestamp(LocalDateTime.now()),
          dto.id);

      return result;
    } finally {
      db.release();
    }
  }

  /**
   * Aggiorna solo lo stato di una chiamata tramite uuid.
   * Chiamato frequentemente durante il ciclo di vita della chiamata
   * (started → ringing → answered → completed).
   */
  public int updateStatus(String uuid, String status) throws Exception
  {
    DB db;
    String sql;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "UPDATE voice_calls SET status = ?, updated_at = ? WHERE uuid = ?";
      result = db.query(sql, status, DB.toSqlTimestamp(LocalDateTime.now()), uuid);

      return result;
    } finally {
      db.release();
    }
  }

  /**
   * Cerca una chiamata per id interno.
   */
  public Optional<CallDto3> findById(Long id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<CallDto3> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT * FROM voice_calls WHERE id = ?";
      rs = db.select(sql, id);

      if (rs.isEmpty()) {
        result = Optional.empty();
      } else {
        result = Optional.of(mapRecord(rs.get(0)));
      }

      return result;
    } finally {
      db.release();
    }
  }

  /**
   * Cerca una chiamata per uuid Vonage.
   * Usato principalmente durante la gestione degli eventi webhook
   * per verificare se la chiamata è già registrata nel DB.
   */
  public Optional<CallDto3> findByUuid(String uuid) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<CallDto3> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT * FROM voice_calls WHERE uuid = ?";
      rs = db.select(sql, uuid);

      if (rs.isEmpty()) {
        result = Optional.empty();
      } else {
        result = Optional.of(mapRecord(rs.get(0)));
      }

      return result;
    } finally {
      db.release();
    }
  }

  /**
   * Restituisce tutte le chiamate ordinate per data di creazione decrescente.
   */
  public List<CallDto3> findAll() throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    List<CallDto3> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM voice_calls ORDER BY created_at DESC";
      rs = db.select(sql);

      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }

      return result;
    } finally {
      db.release();
    }
  }

  /**
   * Mappa un record del ResultSet nel DTO corrispondente.
   * Utilizza i metodi di conversione della libreria DB per gestire
   * i tipi SQLite (Long, Integer, LocalDateTime, etc.).
   */
  private CallDto3 mapRecord(HashMap<String, Object> r)
  {
    CallDto3 dto;

    dto = new CallDto3(
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
        DB.toLong(r.get("campagna_id")),
        DB.toLong(r.get("contatto_id")),
        DB.toLocalDateTime(r.get("created_at")),
        DB.toLocalDateTime(r.get("updated_at")));

    return dto;
  }
}
