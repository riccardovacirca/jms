package com.example.auth.dto;

/** Dati utente completi restituiti dal DB durante i flussi di autenticazione.
 *  Include passwordHash ed email, usati internamente — non esporre nella response. */
public record UserAuthDTO(
  int     id,
  String  username,
  String  passwordHash,
  String  ruolo,
  boolean canAdmin,
  boolean canWrite,
  boolean canDelete,
  boolean mustChangePassword,
  boolean twoFactorEnabled,
  String  email
) {}
