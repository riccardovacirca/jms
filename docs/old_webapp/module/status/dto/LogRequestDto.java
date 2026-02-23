package dev.crm.module.status.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LogRequestDto
{
  @NotBlank(message = "Message cannot be blank")
  @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
  private String message;

  public LogRequestDto()
  {
  }

  public LogRequestDto(String message)
  {
    this.message = message;
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }
}
