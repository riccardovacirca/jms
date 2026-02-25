package {{APP_PACKAGE}}.auth.dto;

import java.time.LocalDateTime;

/** PIN temporaneo di autenticazione a due fattori, letto dalla tabella auth_pins. */
public record AuthPinDTO(
  long          id,
  int           userId,
  String        pinHash,
  LocalDateTime expiresAt
) {}
