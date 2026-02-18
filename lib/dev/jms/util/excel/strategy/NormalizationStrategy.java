package dev.jms.util.excel.strategy;

import java.util.Map;

/**
 * Strategia per normalizzare i valori di un record Excel. Può implementare trasformazioni comuni:
 * trim, lowercase, parsing date, ecc.
 */
@FunctionalInterface
public interface NormalizationStrategy
{

  /**
   * Applica la normalizzazione a un record.
   *
   * @param row
   *          record raw (chiavi già mappate)
   * @return nuovo record normalizzato
   */
  Map<String, Object> normalize(Map<String, Object> row);

  /** Helper di default: ritorna il record invariato */
  default Map<String, Object> identity(Map<String, Object> row)
  {
    return row;
  }
}
