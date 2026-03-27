package dev.jms.app.user.dto;

/** Dati account completi restituiti dal DB durante i flussi di autenticazione.
 *  Include passwordHash ed email, usati internamente — non esporre nella response. */
public record AccountAuthDTO(
  int     id,
  String  username,
  String  passwordHash,
  String  ruolo,
  int     ruoloLevel,
  boolean mustChangePassword,
  boolean twoFactorEnabled,
  String  email
) {}
