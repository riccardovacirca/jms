package dev.jms.app.user.dto;

/** Dati account da restituire al client dopo autenticazione completata.
 *  Usato come payload della response di login, 2FA, refresh e session. */
public record AuthenticatedAccountDTO(
  int     id,
  String  username,
  String  ruolo,
  int     ruoloLevel,
  boolean mustChangePassword
) {}
