package {{APP_PACKAGE}}.user.dto;

/** Credenziali per la verifica del PIN 2FA. */
public record TwoFactorCredentialDTO(String challengeToken, String pin) {}
