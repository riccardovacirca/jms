package dev.crm.module.voice.dto;

public class CreateCallRequestDto
{
  public String toNumber;
  public String operatorType;   // "phone" or "app"
  public String operatorId;     // phone number when type=phone, Client SDK user ID when type=app
  public Long operatorIdCrm;    // CRM operator ID for tracking
  public Long campagnaId;       // Campaign ID
  public Long contattoId;       // Contact ID

  public CreateCallRequestDto()
  {
  }
}
