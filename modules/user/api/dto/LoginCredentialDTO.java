package {{APP_PACKAGE}}.user.dto;

/** Credenziali di login (username + password). */
public record LoginCredentialDTO(String username, String password) {}
