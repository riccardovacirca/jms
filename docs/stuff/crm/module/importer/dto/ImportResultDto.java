package dev.crm.module.importer.dto;

public class ImportResultDto
{
  private int rowsImported;
  public String listaName;

  public ImportResultDto()
  {
  }

  public ImportResultDto(int rowsImported)
  {
    this.rowsImported = rowsImported;
  }

  public int getRowsImported()
  {
    return rowsImported;
  }

  public void setRowsImported(int rowsImported)
  {
    this.rowsImported = rowsImported;
  }

  public String getListaName()
  {
    return listaName;
  }

  public void setListaName(String listaName)
  {
    this.listaName = listaName;
  }
}
