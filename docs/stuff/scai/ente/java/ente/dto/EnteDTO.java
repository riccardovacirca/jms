package dev.jms.app.ente.dto;

/**
 * Rappresenta l'anagrafica di un ente regionale.
 */
public record EnteDTO(
  Long    id,
  String  codEnte,          // max 15
  String  descrizioneEnte,  // max 255
  Integer flagAreas,        // 0 o 1
  Long    idAziendaAreas,   // ID azienda AREAS
  String  createdAt,        // ISO-8601 timestamp
  Long    createdBy,        // User ID
  String  updatedAt,        // ISO-8601 timestamp
  Long    updatedBy         // User ID
) {}
