package dev.jms.app.sdc.dto;

/**
 * Rappresenta un sistema di campo.
 */
public record SdcDTO(
  Long    id,
  String  code,              // max 8 - Codice legacy
  String  slug,              // max 64 - Identificatore univoco
  String  descrizioneBreve,  // max 255
  String  descrizioneLunga,  // max 512
  String  createdAt,         // ISO-8601 timestamp
  String  updatedAt          // ISO-8601 timestamp
) {}
