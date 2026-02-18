package dev.springtools.util.excel.strategy;

import java.util.Map;

public class DefaultMappingStrategy implements MappingStrategy
{

  @Override
  public String mapHeader(String header)
  {
    // identità: restituisce il nome della colonna così com’è
    return header;
  }

  // rimuovi mapHeader(Map<String,String>) se non esiste nell’interfaccia
  // oppure implementalo senza @Override se vuoi un metodo di utilità
  public Map<String, String> mapHeaders(Map<String, String> headers)
  {
    return headers; // restituisce mappa invariata
  }
}
