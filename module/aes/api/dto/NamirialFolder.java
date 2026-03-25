package dev.jms.app.module.aes.dto;

import java.util.List;

/**
 * DTO che rappresenta un folder sulla piattaforma Namirial.
 */
public final class NamirialFolder
{
  public final String id;
  public final String title;
  public final List<String> parents;

  public NamirialFolder(String id, String title, List<String> parents)
  {
    this.id = id;
    this.title = title;
    this.parents = parents;
  }
}
