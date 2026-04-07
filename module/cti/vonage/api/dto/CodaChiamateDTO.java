package dev.jms.app.module.cti.vonage.dto;

import java.time.LocalDateTime;

/**
 * DTO per un elemento della coda chiamate CTI ({@code jms_cti_coda_chiamate}).
 *
 * <p>Stati possibili: pending, assigned, completed, failed, cancelled.</p>
 */
public record CodaChiamateDTO(
  Long          id,
  String        contattoJson,
  String        stato,
  Integer       priorita,
  Long          operatoreId,
  LocalDateTime dataInserimento,
  LocalDateTime dataAssegnazione,
  LocalDateTime dataCompletamento,
  String        esito,
  String        note
) {}
