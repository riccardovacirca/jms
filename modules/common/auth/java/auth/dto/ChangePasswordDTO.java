package {{APP_PACKAGE}}.auth.dto;

public record ChangePasswordDTO(String currentPassword, String newPassword) {}
