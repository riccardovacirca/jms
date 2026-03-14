package {{APP_PACKAGE}}.auth.dto;

import java.util.List;

/** Dati account completi restituiti dal DB durante i flussi di autenticazione.
 *  Include passwordHash ed email, usati internamente — non esporre nella response. */
public record AccountAuthDTO(
  int          id,
  String       username,
  String       passwordHash,
  String       ruolo,
  List<String> permissions,
  boolean      mustChangePassword,
  boolean      twoFactorEnabled,
  String       email
) {}
