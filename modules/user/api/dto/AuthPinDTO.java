package {{APP_PACKAGE}}.user.dto;

import java.time.LocalDateTime;

/** PIN temporaneo di autenticazione a due fattori, letto dalla tabella auth_pins. */
public record AuthPinDTO(
  long          id,
  int           accountId,
  String        pinHash,
  LocalDateTime expiresAt
) {}
