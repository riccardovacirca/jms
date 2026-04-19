package dev.jms.app.module.aes.helper;

import dev.jms.util.Config;
import dev.jms.util.PDF;
import dev.jms.util.Validator;
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
  private static final String DEFAULT_TMP_DIR = "/app/storage/aes/tmp";

  private final String tmpDir;

  /** Costruttore. Legge il path di storage temporaneo dalla configurazione. */
  public SignHelper(Config config)
  {
    this.tmpDir = config.get("aes.resources.tmp", DEFAULT_TMP_DIR);
  }
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
   * <p>
   * Valida il placeholder prima dell'elaborazione.
   * </p>
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

    Validator.signPlaceholder(placeholder, "placeholder");

    pdf = PDF.load(new ByteArrayInputStream(fileBytes));
    replaced = pdf.replaceAllPlaceholdersWithSignatureField(placeholder, "firma", width, height);
    outPath = saveTmp(pdf);
    return new Result(replaced, outPath.toString());
  }

  /**
   * Applica campi firma con supporto sequenze automatiche (TAG, TAG0, TAG1, ..., TAG9).
   * <p>
   * Cerca prima il placeholder base (es. TAG), poi TAG0, TAG1, fino a TAG9.
   * Ogni occorrenza viene sostituita con un campo firma univoco.
   * Valida il placeholder prima dell'elaborazione.
   * </p>
   *
   * @param fileBytes   contenuto del documento PDF
   * @param placeholder testo base del placeholder (es. "TAG")
   * @param width       larghezza del campo firma in punti PDF
   * @param height      altezza del campo firma in punti PDF
   * @param maxSequence numero massimo di sequenze (default: 9 per TAG0-TAG9)
   * @return risultato con numero di sostituzioni e path del file output
   * @throws Exception se l'elaborazione del PDF o il salvataggio falliscono
   */
  public Result signWithSequence(byte[] fileBytes, String placeholder, float width, float height, int maxSequence) throws Exception
  {
    PDF pdf;
    int replaced;
    Path outPath;

    Validator.signPlaceholder(placeholder, "placeholder");

    pdf = PDF.load(new ByteArrayInputStream(fileBytes));
    replaced = pdf.replaceAllPlaceholdersWithSignatureFieldSequence(placeholder, "firma", width, height, maxSequence);
    outPath = saveTmp(pdf);
    return new Result(replaced, outPath.toString());
  }

  private Path saveTmp(PDF pdf) throws Exception
  {
    Path dir;
    Path file;

    dir = Path.of(tmpDir);
    Files.createDirectories(dir);
    file = dir.resolve("firma_" + UUID.randomUUID() + ".pdf");
    try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
      pdf.save(fos);
    }
    return file;
  }
}
