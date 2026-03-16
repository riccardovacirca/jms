package {{APP_PACKAGE}}.user.dto;

/** Dati per il reset password tramite token. */
public record ResetPasswordDTO(String token, String password) {}
