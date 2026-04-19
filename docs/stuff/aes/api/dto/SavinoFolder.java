package dev.jms.app.module.aes.dto;

import java.util.List;

/**
 * DTO per una cartella Savino/DM7.
 */
public class SavinoFolder
{
  public final String id;
  public final String title;
  public final List<String> parents;

  public SavinoFolder(String id, String title, List<String> parents)
  {
    this.id = id;
    this.title = title;
    this.parents = parents;
  }
}
