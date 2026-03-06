package dev.crm.module.voice.dao;

import dev.crm.module.voice.dto.CallDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class CallDao
{

  private final DataSource dataSource;

  public CallDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(CallDto dto) throws Exception
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

  public int update(CallDto dto) throws Exception
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

  public Optional<CallDto> findById(Long id) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<CallDto> result;

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

  public Optional<CallDto> findByUuid(String uuid) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    Optional<CallDto> result;

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

  public List<CallDto> findAll() throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    List<CallDto> result;

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

  public List<CallDto> findByStatus(String status) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    List<CallDto> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM voice_calls WHERE status = ? ORDER BY created_at DESC";
      rs = db.select(sql, status);

      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }

      return result;
    } finally {
      db.release();
    }
  }

  public List<CallDto> findByDirection(String direction) throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    List<CallDto> result;

    db = new DB(dataSource);
    try {
      db.acquire();
      result = new ArrayList<>();
      sql = "SELECT * FROM voice_calls WHERE direction = ? ORDER BY created_at DESC";
      rs = db.select(sql, direction);

      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }

      return result;
    } finally {
      db.release();
    }
  }

  public int count() throws Exception
  {
    DB db;
    String sql;
    ArrayList<HashMap<String, Object>> rs;
    int result;

    db = new DB(dataSource);
    try {
      db.acquire();
      sql = "SELECT COUNT(*) as cnt FROM voice_calls";
      rs = db.select(sql);

      if (!rs.isEmpty()) {
        result = DB.toInteger(rs.get(0).get("cnt"));
      } else {
        result = 0;
      }

      return result;
    } finally {
      db.release();
    }
  }

  private CallDto mapRecord(HashMap<String, Object> r)
  {
    CallDto dto;

    dto = new CallDto(
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
