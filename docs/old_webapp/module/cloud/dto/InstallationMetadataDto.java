package dev.crm.module.cloud.dto;

import java.time.LocalDateTime;

public class InstallationMetadataDto
{
  public Long id;
  public String installationId;
  public String installationName;
  public String sharedSecret;
  public String cloudWebhookUrl;
  public Boolean isActive;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public InstallationMetadataDto()
  {
  }

  public InstallationMetadataDto(
    Long id,
    String installationId,
    String installationName,
    String sharedSecret,
    String cloudWebhookUrl,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
  )
  {
    this.id = id;
    this.installationId = installationId;
    this.installationName = installationName;
    this.sharedSecret = sharedSecret;
    this.cloudWebhookUrl = cloudWebhookUrl;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
