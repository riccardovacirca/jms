package dev.jms.app.user.dto;

/** Dati per il reset password tramite token. */
public record ResetPasswordDTO(String token, String password) {}
