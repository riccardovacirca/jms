package dev.crm.module.importer.dto;

import java.util.Map;

public class ValidationIssueDto
{
  public int rowNumber;
  public String severity; // "error", "warning", "info"
  public String type; // "duplicate", "blacklist", "missing_field", "invalid_format"
  public String message;
  public Map<String, Object> rowData;

  public ValidationIssueDto()
  {
  }

  public ValidationIssueDto(
      int rowNumber, String severity, String type, String message, Map<String, Object> rowData)
  {
    this.rowNumber = rowNumber;
    this.severity = severity;
    this.type = type;
    this.message = message;
    this.rowData = rowData;
  }
}
