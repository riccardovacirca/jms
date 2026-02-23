package dev.crm.module.importer.dto;

import java.util.Map;

/** DTO per ricevere il mapping delle colonne dal frontend */
public class ColumnMappingDto
{
  public String sessionId;
  public Map<String, String> columnMapping; // headerName -> fieldName (es: "Nome" -> "nome")

  public ColumnMappingDto()
  {
  }

  public ColumnMappingDto(String sessionId, Map<String, String> columnMapping)
  {
    this.sessionId = sessionId;
    this.columnMapping = columnMapping;
  }
}
