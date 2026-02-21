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

  public static Log get(Class<?> clazz)
  {
    return new Log(clazz);
  }

  public void info(String msg)
  {
    logger.info(msg);
  }

  public void info(String msg, Object... args)
  {
    logger.info(msg, args);
  }

  public void warn(String msg)
  {
    logger.warn(msg);
  }

  public void warn(String msg, Object... args)
  {
    logger.warn(msg, args);
  }

  public void warn(String msg, Throwable t)
  {
    logger.warn(msg, t);
  }

  public void error(String msg)
  {
    logger.error(msg);
  }

  public void error(String msg, Object... args)
  {
    logger.error(msg, args);
  }

  public void error(String msg, Throwable t)
  {
    logger.error(msg, t);
  }

  public void debug(String msg)
  {
    logger.debug(msg);
  }

  public void debug(String msg, Object... args)
  {
    logger.debug(msg, args);
  }
}
