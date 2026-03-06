package {{APP_PACKAGE}}.sede.dto;

/**
 * Rappresenta l'anagrafica di una sede di lavoro.
 */
public record SedeDTO(
  Long    id,
  String  codSede,           // max 10
  String  nome,              // max 255
  String  indirizzo,         // max 512
  String  cap,               // max 5
  String  cityCode,          // max 4 - Codice ISTAT comune
  String  provinceCode,      // max 4 - Codice provincia
  String  istatRegion,       // max 2 - Codice ISTAT regione
  String  nazione,           // max 2 - Codice ISO nazione
  String  descrizioneBreve,  // max 255
  String  descrizioneLunga,  // max 512
  String  createdAt,         // ISO-8601 timestamp
  String  updatedAt          // ISO-8601 timestamp
) {}
