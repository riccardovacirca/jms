package dev.jms.app.cti.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta una chiamata gestita tramite Vonage.
 * Corrisponde alla tabella chiamate nel database.
 */
public record CallDTO(
  Long          id,
  String        uuid,
  String        conversationUuid,
  String        direction,
  String        status,
  String        fromType,
  String        fromNumber,
  String        toType,
  String        toNumber,
  String        rate,
  String        price,
  Integer       duration,
  LocalDateTime startTime,
  LocalDateTime endTime,
  String        network,
  String        answerUrl,
  String        eventUrl,
  String        errorTitle,
  String        errorDetail,
  Long          operatorId,
  Long          contattoId,
  LocalDateTime createdAt,
  LocalDateTime updatedAt
) {}
