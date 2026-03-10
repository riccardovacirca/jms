package {{APP_PACKAGE}}.contatti.dto;

public record ImportSessionDTO(
  String id,
  String filename,
  String filePath,
  int    rowCount,
  String headers,
  String preview,
  String columnMapping,
  String status,
  String errorMessage,
  String createdAt,
  String updatedAt,
  String completedAt
) {}
