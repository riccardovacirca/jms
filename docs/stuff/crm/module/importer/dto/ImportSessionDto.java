package dev.crm.module.importer.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ImportSessionDto
{
  public String sessionId;
  public String filename;
  public List<String> headers;
  public Integer rowCount;
  public List<Map<String, Object>> previewRows;
  public String status;
  public LocalDateTime createdAt;
  public List<String> warnings;

  public ImportSessionDto()
  {
  }

  public ImportSessionDto(
      String sessionId,
      String filename,
      List<String> headers,
      Integer rowCount,
      List<Map<String, Object>> previewRows)
  {
    this.sessionId = sessionId;
    this.filename = filename;
    this.headers = headers;
    this.rowCount = rowCount;
    this.previewRows = previewRows;
    this.status = "uploaded";
    this.createdAt = LocalDateTime.now();
  }
}
