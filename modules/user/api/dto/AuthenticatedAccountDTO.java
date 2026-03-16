package {{APP_PACKAGE}}.user.dto;

import java.util.List;

/** Dati account da restituire al client dopo autenticazione completata.
 *  Usato come payload della response di login, 2FA, refresh e session. */
public record AuthenticatedAccountDTO(
  int          id,
  String       username,
  String       ruolo,
  List<String> permissions,
  boolean      mustChangePassword
) {}
