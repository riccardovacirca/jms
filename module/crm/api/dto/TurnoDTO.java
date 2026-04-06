package dev.jms.app.crm.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta un turno pianificato nella tabella {@code jms_crm_turno}.
 *
 * <p>{@code operatoreId} fa riferimento a {@code jms_cti_operatori.id}.</p>
 */
public record TurnoDTO(
  Long          id,
  Long          operatoreId,
  LocalDateTime turnoInizio,
  LocalDateTime turnoFine,
  String        note,
  Long          creatoDA,
  LocalDateTime dataCreazione,
  Long          modificatoDA,
  LocalDateTime dataModifica
) {}
