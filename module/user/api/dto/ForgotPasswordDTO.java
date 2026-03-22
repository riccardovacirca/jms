package dev.jms.app.user.dto;

/** Dati per la richiesta di recupero password. */
public record ForgotPasswordDTO(String username, String resetLink) {}
