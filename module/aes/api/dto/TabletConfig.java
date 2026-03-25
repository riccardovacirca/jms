package dev.jms.app.module.aes.dto;

/**
 * DTO per la configurazione di un tablet.
 */
public class TabletConfig
{
  public final String tabletName;
  public final String tabletApp;
  public final String tabletDepartment;
  public final String tabletId;

  public TabletConfig(String tabletName, String tabletApp, String tabletDepartment, String tabletId)
  {
    this.tabletName = tabletName;
    this.tabletApp = tabletApp;
    this.tabletDepartment = tabletDepartment;
    this.tabletId = tabletId;
  }
}
