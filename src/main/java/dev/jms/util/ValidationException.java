package dev.jms.util;

/**
 * Eccezione unchecked lanciata da {@link Validator} quando un vincolo di validazione non è soddisfatto.
 */
public class ValidationException extends RuntimeException
{
  /**
   * Crea una nuova ValidationException con il messaggio specificato.
   *
   * @param message descrizione del vincolo violato
   */
  public ValidationException(String message)
  {
    super(message);
  }
}
