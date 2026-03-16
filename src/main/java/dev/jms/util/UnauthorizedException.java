package dev.jms.util;

/**
 * Eccezione lanciata da {@link HttpRequest#requireAuth()} quando
 * il token di accesso è assente o non valido.
 * {@link HandlerAdapter} la intercetta e risponde con HTTP 401.
 */
public class UnauthorizedException extends RuntimeException
{
  /** Costruttore con messaggio. */
  public UnauthorizedException(String message)
  {
    super(message);
  }
}
