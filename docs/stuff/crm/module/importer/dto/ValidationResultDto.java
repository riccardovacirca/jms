package dev.crm.module.importer.dto;

import java.util.List;

public class ValidationResultDto
{
  public String sessionId;
  public int totalRows;
  public int validRows;
  public int warningRows;
  public int errorRows;
  public List<ValidationIssueDto> issues;

  public ValidationResultDto()
  {
  }

  public ValidationResultDto(
      String sessionId,
      int totalRows,
      int validRows,
      int warningRows,
      int errorRows,
      List<ValidationIssueDto> issues)
  {
    this.sessionId = sessionId;
    this.totalRows = totalRows;
    this.validRows = validRows;
    this.warningRows = warningRows;
    this.errorRows = errorRows;
    this.issues = issues;
  }
}
