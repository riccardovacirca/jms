package dev.jms.app.module.aes.dto;

/**
 * DTO per un documento Savino/DM7.
 */
public class SavinoDocument
{
  public final String id;
  public final String fileName;
  public final String mimeType;
  public final String contentBase64;
  public final boolean isSigned;
  public final String documentTypeId;
  public final String userId;

  public SavinoDocument(
    String id,
    String fileName,
    String mimeType,
    String contentBase64,
    boolean isSigned,
    String documentTypeId,
    String userId)
  {
    this.id = id;
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.contentBase64 = contentBase64;
    this.isSigned = isSigned;
    this.documentTypeId = documentTypeId;
    this.userId = userId;
  }
}
