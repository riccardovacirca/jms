package {{APP_PACKAGE}}.user.dto;

/** Dati per la richiesta di recupero password. */
public record ForgotPasswordDTO(String username, String resetLink) {}
