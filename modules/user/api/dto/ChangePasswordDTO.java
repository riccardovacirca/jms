package {{APP_PACKAGE}}.user.dto;

/** Dati per il cambio password autenticato. */
public record ChangePasswordDTO(String currentPassword, String newPassword) {}
