package dev.jms.app.rapporti.dto;

/**
 * Rappresenta la fotografia di un dipendente associata al rapporto.
 */
public record RapportoFotoDTO(
  Integer id,
  String  codEnte,      // max 15
  String  matricola,    // max 15
  byte[]  foto,         // BYTEA - immagine binaria
  String  createdAt,    // ISO-8601 timestamp
  String  updatedAt     // ISO-8601 timestamp
) {}
