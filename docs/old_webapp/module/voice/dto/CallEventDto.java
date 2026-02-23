package dev.crm.module.voice.dto;

import java.time.LocalDateTime;

public class CallEventDto
{
  public Long id;
  public Long callId;
  public String uuid;
  public String conversationUuid;
  public String status;
  public String direction;
  public LocalDateTime timestamp;
  public String fromNumber;
  public String toNumber;
  public String payload;
  public LocalDateTime createdAt;

  public CallEventDto()
  {
  }

  public CallEventDto(
      Long id,
      Long callId,
      String uuid,
      String conversationUuid,
      String status,
      String direction,
      LocalDateTime timestamp,
      String fromNumber,
      String toNumber,
      String payload,
      LocalDateTime createdAt)
  {
    this.id = id;
    this.callId = callId;
    this.uuid = uuid;
    this.conversationUuid = conversationUuid;
    this.status = status;
    this.direction = direction;
    this.timestamp = timestamp;
    this.fromNumber = fromNumber;
    this.toNumber = toNumber;
    this.payload = payload;
    this.createdAt = createdAt;
  }
}
