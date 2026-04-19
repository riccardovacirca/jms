package dev.jms.app.module.aes.dto;

/**
 * DTO che rappresenta il risultato di un upload su piattaforma Namirial.
 */
public final class NamirialUploadResult
{
  public final boolean success;
  public final String documentId;
  public final String message;

  public NamirialUploadResult(boolean success, String documentId, String message)
  {
    this.success = success;
    this.documentId = documentId;
    this.message = message;
  }
}
