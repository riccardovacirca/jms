package {{APP_PACKAGE}}.repertorio.dto;

/**
 * Rappresenta un repertorio.
 */
public record RepertorioDTO(
  Long    id,
  String  codiceRepertorio,  // max 6
  String  descrizione,       // max 255
  String  slugSdc,           // max 32 - FK verso scai_sistemi_campo
  String  livello,           // max 8
  String  flagParcheggio,    // max 4
  String  flagStruttura,     // max 4
  String  createdAt,         // ISO-8601 timestamp
  String  updatedAt          // ISO-8601 timestamp
) {}
