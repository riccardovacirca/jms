package dev.jms.app.module.aes.dto;

/**
 * DTO per il risultato di un upload documento su Savino/DM7.
 */
public class SavinoUploadResult
{
  public final boolean success;
  public final String documentId;
  public final String message;

  public SavinoUploadResult(boolean success, String documentId, String message)
  {
    this.success = success;
    this.documentId = documentId;
    this.message = message;
  }
}
