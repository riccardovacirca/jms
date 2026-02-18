package dev.jms.util.excel;

import dev.jms.util.excel.strategy.MappingStrategy;
import java.util.HashMap;
import java.util.Map;

/** Mappa le colonne di un record Excel verso i campi logici usando la strategia fornita. */
public class ColumnMapper
{

  private final MappingStrategy strategy;

  public ColumnMapper(MappingStrategy strategy)
  {
    this.strategy = strategy != null ? strategy : new dev.jms.util.excel.strategy.DefaultMappingStrategy();
  }

  /**
   * Applica il mapping delle intestazioni al record fornito. Ritorna un nuovo record con chiavi
   * mappate.
   */
  public Map<String, Object> map(Map<String, Object> row)
  {
    Map<String, Object> mapped = new HashMap<>();

    for (Map.Entry<String, Object> entry : row.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      String mappedKey = strategy.mapHeader(key);

      if (mappedKey != null) {
        mapped.put(mappedKey, value);
      }
      // se null -> colonna ignorata
    }

    return mapped;
  }
}
