package dev.jms.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper per la lettura e analisi di file Excel (.xlsx) basato su Apache POI.
 * <p>
 * Espone metodi statici {@link #read(InputStream)} e {@link #analyze(InputStream, int)};
 * per importazioni con mapping e normalizzazione usare {@link Importer}.
 * </p>
 */
public final class Excel
{
  private Excel()
  {
  }

  /**
   * Legge tutte le righe dati di un file Excel.
   * La prima riga è trattata come intestazione; le colonne senza intestazione vengono ignorate.
   *
   * @param in stream del file Excel
   * @return lista di record con chiave = header colonna, valore = cella tipizzata
   * @throws Exception se il file non è valido o mancano le intestazioni
   */
  public static List<Map<String, Object>> read(InputStream in) throws Exception
  {
    List<Map<String, Object>> rows;
    List<String> headers;
    List<Integer> validCols;

    rows = new ArrayList<>();
    headers = new ArrayList<>();
    validCols = new ArrayList<>();

    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet;
      Iterator<Row> iter;
      Row headerRow;
      int totalColumns;
      int maxHeaderIndex;
      int rowNumber;

      sheet = wb.getSheetAt(0);
      iter = sheet.iterator();

      if (iter.hasNext()) {
        headerRow = iter.next();
        totalColumns = headerRow.getLastCellNum();

        for (int i = 0; i < totalColumns; i++) {
          Cell cell;
          String header;
          cell = headerRow.getCell(i);
          header = "";
          if (cell != null) {
            try {
              header = cell.getStringCellValue().trim();
            } catch (Exception ignored) {
            }
          }
          if (!header.isEmpty()) {
            headers.add(header);
            validCols.add(i);
          }
        }

        if (headers.isEmpty()) {
          throw new Exception("Il file non contiene intestazioni valide nella prima riga");
        }

        maxHeaderIndex = validCols.get(validCols.size() - 1);
        rowNumber = 1;

        while (iter.hasNext()) {
          Row row;
          int rowLastCell;
          Map<String, Object> record;

          row = iter.next();
          rowNumber++;
          rowLastCell = row.getLastCellNum();

          if (rowLastCell > 0 && rowLastCell < maxHeaderIndex + 1) {
            throw new Exception(String.format(
              "Errore di struttura: riga %d ha %d colonne, necessarie almeno %d.",
              rowNumber, rowLastCell, maxHeaderIndex + 1));
          }

          record = new HashMap<>();
          for (int i = 0; i < headers.size(); i++) {
            Cell cell;
            cell = row.getCell(validCols.get(i), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            record.put(headers.get(i), readCellTyped(cell));
          }
          rows.add(record);
        }
      }
    }

    return rows;
  }

  /**
   * Analizza un file Excel restituendo intestazioni, anteprima e conteggio righe.
   *
   * @param in              stream del file Excel
   * @param previewRowCount numero massimo di righe di anteprima
   * @return risultato dell'analisi
   * @throws Exception se il file non è valido o mancano le intestazioni
   */
  public static AnalysisResult analyze(InputStream in, int previewRowCount) throws Exception
  {
    List<String> headers;
    List<Integer> validCols;
    List<Integer> emptyCols;
    List<Map<String, Object>> preview;
    List<String> warnings;
    int totalRows;
    AnalysisResult result;

    headers = new ArrayList<>();
    validCols = new ArrayList<>();
    emptyCols = new ArrayList<>();
    preview = new ArrayList<>();
    warnings = new ArrayList<>();
    totalRows = 0;

    try (Workbook wb = WorkbookFactory.create(in)) {
      Sheet sheet;
      Iterator<Row> iter;
      Row headerRow;
      int totalColumns;
      int maxHeaderIndex;

      sheet = wb.getSheetAt(0);
      iter = sheet.iterator();

      if (iter.hasNext()) {
        headerRow = iter.next();
        totalColumns = headerRow.getLastCellNum();

        for (int i = 0; i < totalColumns; i++) {
          Cell cell;
          String header;
          cell = headerRow.getCell(i);
          header = "";
          if (cell != null) {
            try {
              header = cell.getStringCellValue().trim();
            } catch (Exception ignored) {
            }
          }
          if (header.isEmpty()) {
            emptyCols.add(i);
          } else {
            headers.add(header);
            validCols.add(i);
          }
        }

        if (headers.isEmpty()) {
          throw new Exception("Il file non contiene intestazioni valide nella prima riga");
        }

        if (!emptyCols.isEmpty()) {
          StringBuilder sb;
          sb = new StringBuilder();
          for (int idx : emptyCols) {
            if (sb.length() > 0) {
              sb.append(", ");
            }
            sb.append((char) ('A' + idx));
          }
          warnings.add(String.format(
            "Rilevate %d colonne senza intestazione (colonne: %s). Verranno ignorate.",
            emptyCols.size(), sb.toString()));
        }

        maxHeaderIndex = validCols.get(validCols.size() - 1);

        while (iter.hasNext()) {
          Row row;
          int rowLastCell;

          row = iter.next();
          totalRows++;
          rowLastCell = row.getLastCellNum();

          if (totalRows <= 20 && rowLastCell < maxHeaderIndex + 1) {
            throw new Exception(String.format(
              "Errore di struttura: riga %d ha solo %d colonne, necessarie almeno %d.",
              totalRows + 1, rowLastCell, maxHeaderIndex + 1));
          }

          if (preview.size() < previewRowCount) {
            Map<String, Object> record;
            record = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
              Cell cell;
              cell = row.getCell(validCols.get(i), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
              record.put(headers.get(i), readCellAsString(cell));
            }
            preview.add(record);
          }
        }
      }
    }

    result = new AnalysisResult(headers, preview, totalRows, warnings);
    return result;
  }

  private static Object readCellTyped(Cell cell)
  {
    Object value;
    double d;

    d = 0;
    if (cell == null) {
      value = null;
    } else {
      switch (cell.getCellType()) {
        case STRING:
          value = cell.getStringCellValue();
          break;
        case NUMERIC:
          if (DateUtil.isCellDateFormatted(cell)) {
            value = cell.getLocalDateTimeCellValue();
          } else {
            d = cell.getNumericCellValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
              value = (long) d;
            } else {
              value = d;
            }
          }
          break;
        case BOOLEAN:
          value = cell.getBooleanCellValue();
          break;
        case FORMULA:
          value = cell.getCellFormula();
          break;
        default:
          value = null;
      }
    }
    return value;
  }

  private static Object readCellAsString(Cell cell)
  {
    Object value;
    double d;

    value = null;
    d = 0;
    if (cell != null) {
      try {
        switch (cell.getCellType()) {
          case STRING:
            value = cell.getStringCellValue();
            break;
          case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
              value = cell.getLocalDateTimeCellValue().toString();
            } else {
              d = cell.getNumericCellValue();
              if (d == (long) d) {
                value = String.valueOf((long) d);
              } else {
                value = String.valueOf(d);
              }
            }
            break;
          case BOOLEAN:
            value = cell.getBooleanCellValue();
            break;
          case FORMULA:
            value = cell.getCellFormula();
            break;
          default:
            value = null;
        }
      } catch (Exception ignored) {
      }
    }
    return value;
  }

  /**
   * Risultato dell'analisi di un file Excel.
   */
  public static class AnalysisResult
  {
    /** Intestazioni delle colonne valide. */
    public final List<String> headers;
    /** Righe di anteprima (al massimo {@code previewRowCount}). */
    public final List<Map<String, Object>> previewRows;
    /** Numero totale di righe dati (esclusa intestazione). */
    public final int totalRows;
    /** Eventuali avvisi (es. colonne senza intestazione). */
    public final List<String> warnings;

    AnalysisResult(
      List<String> headers,
      List<Map<String, Object>> previewRows,
      int totalRows,
      List<String> warnings)
    {
      this.headers = headers;
      this.previewRows = previewRows;
      this.totalRows = totalRows;
      this.warnings = warnings;
    }
  }

  /**
   * Risultato di un'importazione: numero di righe importate con successo.
   */
  public static class ImportResult
  {
    private final int rowsImported;

    ImportResult(int rowsImported)
    {
      this.rowsImported = rowsImported;
    }

    /**
     * Restituisce il numero di righe importate con successo.
     */
    public int getRowsImported()
    {
      return rowsImported;
    }
  }

  /**
   * Consumatore funzionale per le righe elaborate durante l'importazione.
   */
  @FunctionalInterface
  public interface RowConsumer
  {
    /** Elabora una riga normalizzata del file Excel. */
    void accept(Map<String, Object> row) throws Exception;
  }

  /**
   * Mappa un'intestazione Excel al nome del campo logico.
   * Restituisce {@code null} per ignorare la colonna.
   */
  @FunctionalInterface
  public interface MappingStrategy
  {
    /** Restituisce il nome del campo logico, oppure {@code null} per ignorare la colonna. */
    String mapHeader(String excelHeader);
  }

  /**
   * Trasforma i valori di un record già mappato (trim, lowercase, parsing, ecc.).
   */
  @FunctionalInterface
  public interface NormalizationStrategy
  {
    /** Applica la normalizzazione al record e restituisce il record trasformato. */
    Map<String, Object> normalize(Map<String, Object> row);
  }

  /**
   * Esecutore di importazioni Excel con mapping delle colonne e normalizzazione dei valori.
   */
  public static class Importer
  {
    private final InputStream source;
    private final MappingStrategy mappingStrategy;
    private final NormalizationStrategy normalizationStrategy;

    /**
     * @param source                stream del file Excel
     * @param mappingStrategy       mapping colonne verso campi logici; {@code null} = identità
     * @param normalizationStrategy normalizzazione valori; {@code null} = identità
     */
    public Importer(
      InputStream source,
      MappingStrategy mappingStrategy,
      NormalizationStrategy normalizationStrategy)
    {
      this.source = source;
      this.mappingStrategy = mappingStrategy;
      this.normalizationStrategy = normalizationStrategy;
    }

    /**
     * Esegue l'importazione: legge il file, applica mapping e normalizzazione,
     * invoca {@code consumer} per ogni riga elaborata.
     *
     * @param consumer elabora ogni riga normalizzata
     * @return risultato con conteggio righe importate
     * @throws Exception se la lettura del file o il consumer falliscono
     */
    public ImportResult execute(RowConsumer consumer) throws Exception
    {
      List<Map<String, Object>> rows;
      int imported;

      rows = Excel.read(source);
      imported = 0;
      for (Map<String, Object> row : rows) {
        Map<String, Object> mapped;
        Map<String, Object> normalized;
        mapped = applyMapping(row);
        normalized = applyNormalization(mapped);
        consumer.accept(normalized);
        imported++;
      }
      return new ImportResult(imported);
    }

    private Map<String, Object> applyMapping(Map<String, Object> row)
    {
      Map<String, Object> result;
      String key;

      result = new HashMap<>();
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        key = mappingStrategy != null ? mappingStrategy.mapHeader(entry.getKey()) : entry.getKey();
        if (key != null) {
          result.put(key, entry.getValue());
        }
      }
      return result;
    }

    private Map<String, Object> applyNormalization(Map<String, Object> row)
    {
      Map<String, Object> result;
      result = normalizationStrategy != null ? normalizationStrategy.normalize(row) : row;
      return result;
    }
  }
}
