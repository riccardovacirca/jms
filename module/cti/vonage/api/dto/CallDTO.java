package dev.jms.app.module.cti.vonage.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta una chiamata gestita tramite Vonage.
 * Corrisponde alla tabella chiamate nel database.
 */
public record CallDTO(
  Long id,
  String uuid,
  String conversazioneUuid,
  String conversationName,
  String direzione,
  String stato,
  String tipoMittente,
  String numeroMittente,
  String tipoDestinatario,
  String numeroDestinatario,
  String tariffa,
  String costo,
  Integer durata,
  LocalDateTime oraInizio,
  LocalDateTime oraFine,
  String rete,
  String answerUrl,
  String eventUrl,
  String erroreTitolo,
  String erroreDettaglio,
  Long operatoreId,
  Long chiamanteAccountId,
  Long contattoId,
  String callbackUrl,
  String recordingUrl,
  String recordingUuid,
  String recordingPath,
  LocalDateTime dataCreazione,
  LocalDateTime dataAggiornamento
) {}
