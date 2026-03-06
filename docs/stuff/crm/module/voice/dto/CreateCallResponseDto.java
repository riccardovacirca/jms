package dev.crm.module.voice.dto;

public class CreateCallResponseDto
{
  public String uuid;
  public String status;
  public String direction;
  public String conversationUuid;

  public CreateCallResponseDto()
  {
  }

  public CreateCallResponseDto(
      String uuid, String status, String direction, String conversationUuid)
  {
    this.uuid = uuid;
    this.status = status;
    this.direction = direction;
    this.conversationUuid = conversationUuid;
  }
}
