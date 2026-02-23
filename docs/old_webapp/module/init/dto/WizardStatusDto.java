package dev.crm.module.init.dto;

public class WizardStatusDto
{
  public Boolean completed;
  public String completedAt;
  public Boolean hasAzienda;
  public Boolean hasOwnerAccount;
  public Integer totalSedi;
  public Integer totalAccounts;

  public WizardStatusDto()
  {
  }

  public WizardStatusDto(
      Boolean completed,
      String completedAt,
      Boolean hasAzienda,
      Boolean hasOwnerAccount,
      Integer totalSedi,
      Integer totalAccounts)
  {
    this.completed = completed;
    this.completedAt = completedAt;
    this.hasAzienda = hasAzienda;
    this.hasOwnerAccount = hasOwnerAccount;
    this.totalSedi = totalSedi;
    this.totalAccounts = totalAccounts;
  }
}
