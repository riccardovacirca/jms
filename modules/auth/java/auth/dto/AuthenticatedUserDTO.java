package com.example.auth.dto;

/** Dati utente da restituire al client dopo autenticazione completata.
 *  Usato come payload della response di login, 2FA, refresh e session. */
public record AuthenticatedUserDTO(
  int     id,
  String  username,
  String  ruolo,
  boolean canAdmin,
  boolean canWrite,
  boolean canDelete,
  boolean mustChangePassword
) {}
