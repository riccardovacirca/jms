package dev.jms.util.excel.strategy;


public class DefaultMappingStrategy implements MappingStrategy
{

  @Override
  public String mapHeader(String header)
  {
    // identità: restituisce il nome della colonna così com’è
    return header;
  }

}
