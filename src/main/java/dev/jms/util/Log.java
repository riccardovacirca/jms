package dev.jms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper di logging basato su SLF4J + Logback.
 * Uso: private static final Log log = Log.get(MiaClasse.class);
 */
public class Log
{
  private final Logger logger;

  private Log(Class<?> clazz)
  {
    this.logger = LoggerFactory.getLogger(clazz);
  }

  /**
   * Restituisce una nuova istanza di Log associata alla classe specificata.
   *
   * @param clazz classe da usare come nome logger
   * @return istanza Log
   */
  public static Log get(Class<?> clazz)
  {
    return new Log(clazz);
  }

  /**
   * Logga un messaggio a livello INFO.
   *
   * @param msg messaggio da loggare
   */
  public void info(String msg)
  {
    logger.info(msg);
  }

  /**
   * Logga un messaggio a livello INFO con argomenti SLF4J.
   *
   * @param msg  pattern SLF4J (es. {@code "Valore: {}"})
   * @param args argomenti da sostituire nel pattern
   */
  public void info(String msg, Object... args)
  {
    logger.info(msg, args);
  }

  /**
   * Logga un messaggio a livello WARN.
   *
   * @param msg messaggio da loggare
   */
  public void warn(String msg)
  {
    logger.warn(msg);
  }

  /**
   * Logga un messaggio a livello WARN con argomenti SLF4J.
   *
   * @param msg  pattern SLF4J
   * @param args argomenti da sostituire nel pattern
   */
  public void warn(String msg, Object... args)
  {
    logger.warn(msg, args);
  }

  /**
   * Logga un messaggio a livello WARN con stack trace.
   *
   * @param msg messaggio da loggare
   * @param t   eccezione da includere
   */
  public void warn(String msg, Throwable t)
  {
    logger.warn(msg, t);
  }

  /**
   * Logga un messaggio a livello ERROR.
   *
   * @param msg messaggio da loggare
   */
  public void error(String msg)
  {
    logger.error(msg);
  }

  /**
   * Logga un messaggio a livello ERROR con argomenti SLF4J.
   *
   * @param msg  pattern SLF4J
   * @param args argomenti da sostituire nel pattern
   */
  public void error(String msg, Object... args)
  {
    logger.error(msg, args);
  }

  /**
   * Logga un messaggio a livello ERROR con stack trace.
   *
   * @param msg messaggio da loggare
   * @param t   eccezione da includere
   */
  public void error(String msg, Throwable t)
  {
    logger.error(msg, t);
  }

  /**
   * Logga un messaggio a livello DEBUG.
   *
   * @param msg messaggio da loggare
   */
  public void debug(String msg)
  {
    logger.debug(msg);
  }

  /**
   * Logga un messaggio a livello DEBUG con argomenti SLF4J.
   *
   * @param msg  pattern SLF4J
   * @param args argomenti da sostituire nel pattern
   */
  public void debug(String msg, Object... args)
  {
    logger.debug(msg, args);
  }
}
