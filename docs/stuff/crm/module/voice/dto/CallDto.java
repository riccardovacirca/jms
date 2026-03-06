package dev.crm.module.voice.dto;

import java.time.LocalDateTime;

public class CallDto
{
  public Long id;
  public String uuid;
  public String conversationUuid;
  public String direction;
  public String status;
  public String fromType;
  public String fromNumber;
  public String toType;
  public String toNumber;
  public String rate;
  public String price;
  public Integer duration;
  public LocalDateTime startTime;
  public LocalDateTime endTime;
  public String network;
  public String answerUrl;
  public String eventUrl;
  public String errorTitle;
  public String errorDetail;
  public Long operatorId;
  public Long campagnaId;
  public Long contattoId;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public CallDto()
  {
  }

  public CallDto(
      Long id,
      String uuid,
      String conversationUuid,
      String direction,
      String status,
      String fromType,
      String fromNumber,
      String toType,
      String toNumber,
      String rate,
      String price,
      Integer duration,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String network,
      String answerUrl,
      String eventUrl,
      String errorTitle,
      String errorDetail,
      Long operatorId,
      Long campagnaId,
      Long contattoId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.uuid = uuid;
    this.conversationUuid = conversationUuid;
    this.direction = direction;
    this.status = status;
    this.fromType = fromType;
    this.fromNumber = fromNumber;
    this.toType = toType;
    this.toNumber = toNumber;
    this.rate = rate;
    this.price = price;
    this.duration = duration;
    this.startTime = startTime;
    this.endTime = endTime;
    this.network = network;
    this.answerUrl = answerUrl;
    this.eventUrl = eventUrl;
    this.errorTitle = errorTitle;
    this.errorDetail = errorDetail;
    this.operatorId = operatorId;
    this.campagnaId = campagnaId;
    this.contattoId = contattoId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
