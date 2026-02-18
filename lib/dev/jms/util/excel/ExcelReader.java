package dev.springtools.util.excel;

import java.io.InputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;

public class ExcelReader
{

  private final InputStream excel;

  public ExcelReader(InputStream excel)
  {
    this.excel = excel;
  }

  public List<Map<String, Object>> read() throws Exception
  {

    List<Map<String, Object>> rows = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(excel)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = sheet.iterator();

      if (!iterator.hasNext()) {
        return rows;
      }

      // Header - traccia gli indici delle colonne valide
      Row headerRow = iterator.next();
      List<String> headers = new ArrayList<>();
      List<Integer> validColumnIndices = new ArrayList<>();
      int totalColumns = headerRow.getLastCellNum();

      // Itera su tutte le colonne usando indici (non solo celle esistenti)
      for (int columnIndex = 0; columnIndex < totalColumns; columnIndex++) {
        Cell cell = headerRow.getCell(columnIndex);
        String header = "";

        if (cell != null) {
          try {
            header = cell.getStringCellValue().trim();
          } catch (Exception e) {
            // Se non è una stringa, ignora
          }
        }

        if (!header.isEmpty()) {
          headers.add(header);
          validColumnIndices.add(columnIndex);
        }
      }

      // Verifica che ci siano header
      if (headers.isEmpty()) {
        throw new Exception("Il file non contiene intestazioni valide nella prima riga");
      }

      // Data rows - leggi solo le colonne valide
      int maxHeaderIndex = validColumnIndices.isEmpty() ? -1 : validColumnIndices.get(validColumnIndices.size() - 1);
      int rowNumber = 1;

      while (iterator.hasNext()) {
        Row row = iterator.next();
        rowNumber++;

        // Verifica che la riga abbia abbastanza colonne per tutte le colonne con header
        if (maxHeaderIndex >= 0) {
          int rowColumns = row.getLastCellNum();
          if (rowColumns > 0 && rowColumns < maxHeaderIndex + 1) {
            throw new Exception(
                String.format(
                    "Errore di struttura del file: la riga %d ha solo %d colonne, "
                        + "ma sono necessarie almeno %d colonne per le intestazioni presenti. "
                        + "Verificare che tutte le righe abbiano dati per tutte le colonne.",
                    rowNumber,
                    rowColumns,
                    maxHeaderIndex + 1));
          }
        }

        Map<String, Object> record = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
          int actualColumnIndex = validColumnIndices.get(i);
          Cell cell = row.getCell(actualColumnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
          record.put(headers.get(i), readCell(cell));
        }

        rows.add(record);
      }
    }

    return rows;
  }

  private Object readCell(Cell cell)
  {
    if (cell == null) {
      return null;
    }

    switch (cell.getCellType()) {
      case STRING :
        return cell.getStringCellValue();
      case NUMERIC :
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getLocalDateTimeCellValue();
        }
        return cell.getNumericCellValue();
      case BOOLEAN :
        return cell.getBooleanCellValue();
      case FORMULA :
        return cell.getCellFormula();
      default :
        return null;
    }
  }
}
