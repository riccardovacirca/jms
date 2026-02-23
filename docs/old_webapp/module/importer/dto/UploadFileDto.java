package dev.crm.module.importer.dto;

public class UploadFileDto
{
  private String path; // path file temporaneo o già presente
  private String originalName;

  public UploadFileDto()
  {
  }

  public UploadFileDto(String path, String originalName)
  {
    this.path = path;
    this.originalName = originalName;
  }

  public String getPath()
  {
    return path;
  }

  public String getOriginalName()
  {
    return originalName;
  }

  public void setPath(String path)
  {
    this.path = path;
  }

  public void setOriginalName(String originalName)
  {
    this.originalName = originalName;
  }
}
