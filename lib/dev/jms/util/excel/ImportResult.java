package dev.jms.util.excel;

/** Rappresenta l'esito di un'importazione Excel. */
public class ImportResult
{

  private final int rowsImported;

  public ImportResult(int rowsImported)
  {
    this.rowsImported = rowsImported;
  }

  public int getRowsImported()
  {
    return rowsImported;
  }

  @Override
  public String toString()
  {
    return "ImportResult{" + "rowsImported=" + rowsImported + '}';
  }
}
