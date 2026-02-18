package dev.springtools.util.excel;

import dev.springtools.util.excel.strategy.MappingStrategy;
import dev.springtools.util.excel.strategy.NormalizationStrategy;
import java.util.Map;

public class ImportConfig
{

  /** Mappa esplicita: header Excel → campo logico */
  private Map<String, String> columnMapping;

  /** Strategie (opzionali) */
  private MappingStrategy mappingStrategy;

  private NormalizationStrategy normalizationStrategy;

  /** Opzioni di comportamento */
  private boolean ignoreUnknownColumns = true;

  private boolean skipEmptyRows = true;

  // =====================
  // Getter / Setter
  // =====================

  public Map<String, String> getColumnMapping()
  {
    return columnMapping;
  }

  public void setColumnMapping(Map<String, String> columnMapping)
  {
    this.columnMapping = columnMapping;
  }

  public MappingStrategy getMappingStrategy()
  {
    return mappingStrategy;
  }

  public void setMappingStrategy(MappingStrategy mappingStrategy)
  {
    this.mappingStrategy = mappingStrategy;
  }

  public NormalizationStrategy getNormalizationStrategy()
  {
    return normalizationStrategy;
  }

  public void setNormalizationStrategy(NormalizationStrategy normalizationStrategy)
  {
    this.normalizationStrategy = normalizationStrategy;
  }

  public boolean isIgnoreUnknownColumns()
  {
    return ignoreUnknownColumns;
  }

  public void setIgnoreUnknownColumns(boolean ignoreUnknownColumns)
  {
    this.ignoreUnknownColumns = ignoreUnknownColumns;
  }

  public boolean isSkipEmptyRows()
  {
    return skipEmptyRows;
  }

  public void setSkipEmptyRows(boolean skipEmptyRows)
  {
    this.skipEmptyRows = skipEmptyRows;
  }
}
