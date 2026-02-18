package dev.springtools.util.excel;

import dev.springtools.util.excel.strategy.MappingStrategy;
import dev.springtools.util.excel.strategy.NormalizationStrategy;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ExcelImporter
{

  private final ExcelReader reader;
  private final ColumnMapper mapper;
  private final Normalizer normalizer;

  public ExcelImporter(
      InputStream excel,
      MappingStrategy mappingStrategy,
      NormalizationStrategy normalizationStrategy)
  {
    this.reader = new ExcelReader(excel);
    this.mapper = new ColumnMapper(mappingStrategy);
    this.normalizer = new Normalizer(normalizationStrategy);
  }

  public ImportResult execute(RowConsumer consumer) throws Exception
  {
    List<Map<String, Object>> rows = reader.read();
    int imported = 0;

    for (Map<String, Object> row : rows) {
      Map<String, Object> mapped = mapper.map(row);
      Map<String, Object> normalized = normalizer.apply(mapped);
      consumer.accept(normalized);
      imported++;
    }

    return new ImportResult(imported);
  }
}
