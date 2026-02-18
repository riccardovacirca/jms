package dev.jms.util.excel;

import java.io.InputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;

/** Analizza un file Excel per estrarre headers e preview senza processare tutto il file */
public class ExcelAnalyzer
{

  private final InputStream excel;

  public ExcelAnalyzer(InputStream excel)
  {
    this.excel = excel;
  }

  /** Analizza il file e restituisce le informazioni di base */
  public AnalysisResult analyze(int previewRowCount) throws Exception
  {
    List<String> headers = new ArrayList<>();
    List<Integer> validColumnIndices = new ArrayList<>();
    List<Map<String, Object>> previewRows = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    int totalRows = 0;

    try (Workbook workbook = WorkbookFactory.create(excel)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = sheet.iterator();

      if (!iterator.hasNext()) {
        return new AnalysisResult(headers, previewRows, 0, new ArrayList<>());
      }

      // Leggi header (prima riga) e traccia gli indici delle colonne valide
      Row headerRow = iterator.next();
      int totalColumns = headerRow.getLastCellNum();

      // Itera su tutte le colonne usando indici (non solo celle esistenti)
      List<Integer> emptyColumnIndices = new ArrayList<>();

      for (int columnIndex = 0; columnIndex < totalColumns; columnIndex++) {
        Cell cell = headerRow.getCell(columnIndex);
        String header = "";

        if (cell != null) {
          try {
            header = cell.getStringCellValue();
          } catch (Exception e) {
            // Se non è una stringa, ignora
          }
        }

        if (header != null && !header.trim().isEmpty()) {
          headers.add(header.trim());
          validColumnIndices.add(columnIndex);
        } else {
          emptyColumnIndices.add(columnIndex);
        }
      }

      // Aggiungi warning se ci sono colonne senza header
      if (!emptyColumnIndices.isEmpty()) {
        String columnsList = emptyColumnIndices.stream()
            .map(i -> String.valueOf((char)('A' + i)))
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
        warnings.add(
            String.format(
                "Rilevate %d colonne senza intestazione (colonne: %s). "
                    + "Queste colonne verranno ignorate durante l'importazione.",
                emptyColumnIndices.size(),
                columnsList));
      }

      // Verifica che ci siano header
      if (headers.isEmpty()) {
        throw new Exception("Il file non contiene intestazioni valide nella prima riga");
      }

      // Leggi righe di preview e conta il totale
      int maxHeaderIndex = validColumnIndices.isEmpty() ? -1 : validColumnIndices.get(validColumnIndices.size() - 1);

      while (iterator.hasNext()) {
        Row row = iterator.next();
        totalRows++;

        // Verifica che la riga abbia abbastanza colonne per tutte le colonne con header (controlla prime 20 righe)
        if (totalRows <= 20 && maxHeaderIndex >= 0) {
          int rowColumns = row.getLastCellNum();
          if (rowColumns < maxHeaderIndex + 1) {
            throw new Exception(
                String.format(
                    "Errore di struttura del file: la riga %d ha solo %d colonne, "
                        + "ma sono necessarie almeno %d colonne per le intestazioni presenti. "
                        + "Verificare che tutte le righe abbiano dati per tutte le colonne.",
                    totalRows + 1,
                    rowColumns,
                    maxHeaderIndex + 1));
          }
        }

        // Aggiungi solo le prime N righe al preview
        if (previewRows.size() < previewRowCount) {
          Map<String, Object> record = new HashMap<>();
          for (int i = 0; i < headers.size(); i++) {
            int actualColumnIndex = validColumnIndices.get(i);
            Cell cell = row.getCell(actualColumnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            record.put(headers.get(i), readCell(cell));
          }
          previewRows.add(record);
        }
      }
    }

    return new AnalysisResult(headers, previewRows, totalRows, warnings);
  }

  private Object readCell(Cell cell)
  {
    if (cell == null) {
      return null;
    }

    try {
      switch (cell.getCellType()) {
        case STRING :
          return cell.getStringCellValue();
        case NUMERIC :
          if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toString();
          }
          // Formatta numeri per evitare notazione scientifica
          double numValue = cell.getNumericCellValue();
          if (numValue == (long) numValue) {
            return String.valueOf((long) numValue);
          }
          return String.valueOf(numValue);
        case BOOLEAN :
          return cell.getBooleanCellValue();
        case FORMULA :
          return cell.getCellFormula();
        default :
          return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /** Risultato dell'analisi */
  public static class AnalysisResult
  {
    public final List<String> headers;
    public final List<Map<String, Object>> previewRows;
    public final int totalRows;
    public final List<String> warnings;

    public AnalysisResult(
        List<String> headers, List<Map<String, Object>> previewRows, int totalRows, List<String> warnings)
    {
      this.headers = headers;
      this.previewRows = previewRows;
      this.totalRows = totalRows;
      this.warnings = warnings;
    }
  }
}
