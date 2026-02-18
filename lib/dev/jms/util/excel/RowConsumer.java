package dev.springtools.util.excel;

import java.util.Map;

/** Interfaccia funzionale per consumare i record normalizzati. */
@FunctionalInterface
public interface RowConsumer
{
  void accept(Map<String, Object> row) throws Exception;
}
