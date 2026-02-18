package dev.jms.util.excel;

import dev.jms.util.excel.strategy.NormalizationStrategy;
import java.util.Map;

/**
 * Applica la normalizzazione ai record Excel. Utilizza la NormalizationStrategy fornita dall'utente
 * o default.
 */
public class Normalizer
{

  private final NormalizationStrategy strategy;

  public Normalizer(NormalizationStrategy strategy)
  {
    this.strategy = strategy != null ? strategy : row -> row;
  }

  /**
   * Applica la normalizzazione a un singolo record.
   *
   * @param row
   *          record già mappato
   * @return record normalizzato
   */
  public Map<String, Object> apply(Map<String, Object> row)
  {
    return strategy.normalize(row);
  }
}
