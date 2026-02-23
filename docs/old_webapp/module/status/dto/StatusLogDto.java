package dev.crm.module.status.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class StatusLogDto
{
  private Long id;

  @NotBlank(message = "Message cannot be blank")
  @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
  private String message;

  private LocalDateTime createdAt;

  public StatusLogDto()
  {
  }

  public StatusLogDto(Long id, String message, LocalDateTime createdAt)
  {
    this.id = id;
    this.message = message;
    this.createdAt = createdAt;
  }

  public Long getId()
  {
    return id;
  }

  public String getMessage()
  {
    return message;
  }

  public LocalDateTime getCreatedAt()
  {
    return createdAt;
  }
}
