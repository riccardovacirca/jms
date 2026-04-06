package dev.jms.app.module.cti.vonage.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta un turno operatore nella tabella {@code jms_sessione_operatore}.
 *
 * <p>Stato corrente: 0=disconnesso, 1=connesso, 2=in pausa, 3=in chiamata.</p>
 */
public record SessioneOperatoreDTO(
  Long          id,
  Long          operatoreId,
  LocalDateTime turnoInizio,
  LocalDateTime turnoFine,
  LocalDateTime connessioneInizio,
  LocalDateTime connessioneFine,
  Integer       durataTotale,
  Integer       numeroPause,
  Integer       durataPause,
  LocalDateTime ultimaConnessione,
  Integer       numeroChiamate,
  Integer       durataConversazione,
  Integer       stato,
  String        note,
  Long          creatoDA,
  LocalDateTime dataCreazione,
  Long          modificatoDA,
  LocalDateTime dataModifica
) {}
