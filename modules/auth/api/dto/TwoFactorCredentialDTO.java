package {{APP_PACKAGE}}.auth.dto;

public record TwoFactorCredentialDTO(String challengeToken, String pin) {}
