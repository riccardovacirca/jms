package dev.jms.app.module.cti.vonage.dto;

import java.time.LocalDateTime;

/**
 * DTO per un contatto nella coda personale di un operatore CTI ({@code jms_cti_operatore_contatti}).
 */
public record OperatoreContattoDTO(
  Long          id,
  Long          operatoreId,
  String        contattoJson,
  LocalDateTime dataInserimento,
  LocalDateTime pianificatoAl
) {}
