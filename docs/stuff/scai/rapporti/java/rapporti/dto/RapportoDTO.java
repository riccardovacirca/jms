package dev.jms.app.rapporti.dto;

/**
 * Rappresenta un rapporto di lavoro con tutti i suoi campi.
 * Questo DTO corrisponde all'entità scai_rapporto del backend Laravel.
 */
public record RapportoDTO(
  Integer id,
  String  codEnte,              // max 4
  String  matricola,            // max 15
  String  codRapporto,          // max 5
  String  cognome,              // max 30
  String  nome,                 // max 30
  String  sesso,                // M/F
  String  codFis,               // max 16
  String  dataAssunzione,       // ISO-8601 date
  String  dataCessazione,       // ISO-8601 date, nullable
  String  codSedePrimaria,      // max 15
  String  dataInizioSedePrimaria,
  String  codSedeSecondaria,    // max 15, nullable
  String  dataInizioSedeSecondaria,
  String  settore,              // max 30
  String  ufficioPiano,         // max 4
  String  ufficioNumeroStanza,  // max 10
  String  ufficioTelefono,      // max 10
  String  email,                // max 128
  String  urlImage,
  String  pBadge,               // Badge virtuale
  String  codiceStruttura,      // max 16
  String  descrizioneStruttura, // max 255
  int     servizioFuoriSede,    // 0 o 1
  String  status,               // nuovo, aggiornato, chiuso_nel_passato
  String  emailPersonale,
  String  codEnteHr,            // max 3
  String  descrizioneSedePrimaria,    // max 512
  String  descrizioneSedeSecondaria,  // max 512
  String  codiceMansione,       // max 16
  String  descrizioneMansione,  // max 512
  String  codiceAzienda,
  String  descrizioneAzienda,
  String  createdAt,            // ISO-8601 timestamp
  String  updatedAt             // ISO-8601 timestamp
) {}
