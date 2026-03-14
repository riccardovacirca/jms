package {{APP_PACKAGE}}.auth.dto;

/** DTO per la richiesta di reset password tramite token. */
public record ResetPasswordDTO(String token, String password) {}
