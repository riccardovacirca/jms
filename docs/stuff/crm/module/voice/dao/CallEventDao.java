package dev.crm.module.voice.dao;

import dev.crm.module.voice.dto.CallEventDto;
import dev.springtools.util.DB;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

@Repository
public class CallEventDao
{

  private final DataSource dataSource;

  public CallEventDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public long insert(CallEventDto dto) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      String sql = "INSERT INTO voice_events (call_id, uuid, conversation_uuid, status, "
          + "direction, timestamp, from_number, to_number, payload, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      db.query(
          sql,
          dto.callId,
          dto.uuid,
          dto.conversationUuid,
          dto.status,
          dto.direction,
          dto.timestamp != null ? DB.toSqlTimestamp(dto.timestamp) : null,
          dto.fromNumber,
          dto.toNumber,
          dto.payload,
          DB.toSqlTimestamp(LocalDateTime.now()));
      return db.lastInsertId();
    } finally {
      db.release();
    }
  }

  public List<CallEventDto> findByCallId(Long callId) throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<CallEventDto> result = new ArrayList<>();
      String sql = "SELECT * FROM voice_events WHERE call_id = ? ORDER BY timestamp ASC";
      ArrayList<HashMap<String, Object>> rs = db.select(sql, callId);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  public List<CallEventDto> findAll() throws Exception
  {
    DB db = new DB(dataSource);
    try {
      db.acquire();
      List<CallEventDto> result = new ArrayList<>();
      String sql = "SELECT * FROM voice_events ORDER BY timestamp DESC LIMIT 1000";
      ArrayList<HashMap<String, Object>> rs = db.select(sql);
      for (HashMap<String, Object> r : rs) {
        result.add(mapRecord(r));
      }
      return result;
    } finally {
      db.release();
    }
  }

  private CallEventDto mapRecord(HashMap<String, Object> r)
  {
    return new CallEventDto(
        DB.toLong(r.get("id")),
        DB.toLong(r.get("call_id")),
        DB.toString(r.get("uuid")),
        DB.toString(r.get("conversation_uuid")),
        DB.toString(r.get("status")),
        DB.toString(r.get("direction")),
        DB.toLocalDateTime(r.get("timestamp")),
        DB.toString(r.get("from_number")),
        DB.toString(r.get("to_number")),
        DB.toString(r.get("payload")),
        DB.toLocalDateTime(r.get("created_at")));
  }
}
