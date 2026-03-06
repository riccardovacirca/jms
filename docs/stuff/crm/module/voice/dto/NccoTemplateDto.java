package dev.crm.module.voice.dto;

import java.time.LocalDateTime;

public class NccoTemplateDto
{
  public Long id;
  public String name;
  public String description;
  public String nccoJson;
  public Integer isActive;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public NccoTemplateDto()
  {
  }

  public NccoTemplateDto(
      Long id,
      String name,
      String description,
      String nccoJson,
      Integer isActive,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.name = name;
    this.description = description;
    this.nccoJson = nccoJson;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
