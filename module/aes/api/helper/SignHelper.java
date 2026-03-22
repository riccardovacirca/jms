package dev.jms.app.aes.helper;

import dev.jms.util.PDF;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Logica di business per l'apposizione di campi firma in documenti PDF.
 */
public class SignHelper
{
  /**
   * Risultato dell'operazione di firma: occorrenze sostituite e path del file output.
   */
  public static class Result
  {
    /** Numero di placeholder sostituiti con campo firma. */
    public final int replaced;
    /** Path assoluto del file PDF modificato salvato in directory temporanea. */
    public final String outputFile;

    Result(int replaced, String outputFile)
    {
      this.replaced = replaced;
      this.outputFile = outputFile;
    }
  }

  /**
   * Applica un campo firma AcroForm in corrispondenza di ogni occorrenza del placeholder,
   * salva il documento modificato in una directory temporanea e restituisce il risultato.
   *
   * @param fileBytes   contenuto del documento PDF
   * @param placeholder testo da sostituire con il campo firma
   * @param width       larghezza del campo firma in punti PDF
   * @param height      altezza del campo firma in punti PDF
   * @return risultato con numero di sostituzioni e path del file output
   * @throws Exception se l'elaborazione del PDF o il salvataggio falliscono
   */
  public Result sign(byte[] fileBytes, String placeholder, float width, float height) throws Exception
  {
    PDF pdf;
    int replaced;
    Path outPath;

    pdf = PDF.load(new ByteArrayInputStream(fileBytes));
    replaced = pdf.replaceAllPlaceholdersWithSignatureField(placeholder, "firma", width, height);
    outPath = saveTmp(pdf);
    return new Result(replaced, outPath.toString());
  }

  private Path saveTmp(PDF pdf) throws Exception
  {
    Path dir;
    Path file;

    dir = Path.of(System.getProperty("java.io.tmpdir"), "aes");
    Files.createDirectories(dir);
    file = dir.resolve("firma_" + UUID.randomUUID() + ".pdf");
    try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
      pdf.save(fos);
    }
    return file;
  }
}
