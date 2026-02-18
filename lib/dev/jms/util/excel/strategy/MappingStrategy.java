package dev.jms.util.excel.strategy;

import java.util.Map;

/**
 * Strategia per il mapping delle intestazioni Excel verso i campi logici dell'applicazione. Può
 * essere implementata in modo statico (dizionario) o dinamico (ML, fuzzy matching, ecc.).
 */
@FunctionalInterface
public interface MappingStrategy
{

  /**
   * Restituisce il nome del campo logico corrispondente all'header Excel.
   *
   * @param excelHeader
   *          nome colonna Excel
   * @return nome campo logico, oppure null se non mappabile
   */
  String mapHeader(String excelHeader);

  /** Helper per mappare un intero record (opzionale, default) */
  default Map<String, Object> mapRecord(Map<String, Object> row)
  {
    row.replaceAll((key, value) -> value); // placeholder
    return row;
  }
}
