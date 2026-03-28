package dev.jms.util;

import com.lowagie.text.DocumentException;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.ByteArrayOutputStream;

/**
 * Utility per la conversione di HTML in PDF basata su Flying Saucer + OpenPDF.
 * <p>
 * Supporta HTML/XHTML con CSS 2.1. Per risultati ottimali l'HTML deve essere
 * well-formed (XHTML-like). Usa {@link #convert(String)} per conversione base
 * o {@link #convert(String, boolean)} per abilitare preprocessing degli input.
 * </p>
 *
 * <h3>Esempio:</h3>
 * <pre>{@code
 * String html = "<html><body><h1>Titolo</h1><p>Testo</p></body></html>";
 * byte[] pdf = HTML2PDF.convert(html);
 * }</pre>
 */
public final class HTML2PDF
{
  private static final Log log = Log.get(HTML2PDF.class);

  private HTML2PDF()
  {
  }

  /**
   * Converte HTML in PDF.
   *
   * @param html contenuto HTML da convertire (deve essere well-formed)
   * @return contenuto del PDF come array di byte
   * @throws Exception se la conversione fallisce
   */
  public static byte[] convert(String html) throws Exception
  {
    return convert(html, false);
  }

  /**
   * Converte HTML in PDF con preprocessing opzionale.
   *
   * @param html            contenuto HTML da convertire
   * @param preprocessInput se {@code true}, converte tag {@code <input>} in {@code <span>}
   *                        per compatibilità rendering
   * @return contenuto del PDF come array di byte
   * @throws Exception se la conversione fallisce
   */
  public static byte[] convert(String html, boolean preprocessInput) throws Exception
  {
    String processedHtml;
    ITextRenderer renderer;
    ByteArrayOutputStream out;

    if (html == null || html.isBlank()) {
      throw new IllegalArgumentException("HTML content cannot be null or empty");
    }

    processedHtml = html;
    if (preprocessInput) {
      processedHtml = preprocessHtml(html);
    }

    processedHtml = ensureWellFormed(processedHtml);

    renderer = new ITextRenderer();
    out = new ByteArrayOutputStream();

    try {
      renderer.setDocumentFromString(processedHtml);
      renderer.layout();
      renderer.createPDF(out);
      log.debug("HTML converted to PDF successfully ({} bytes)", out.size());
      return out.toByteArray();
    } catch (DocumentException e) {
      log.error("Failed to convert HTML to PDF", e);
      throw new Exception("HTML to PDF conversion failed: " + e.getMessage(), e);
    }
  }

  /**
   * Preprocessing HTML: converte {@code <input>} in {@code <span>} per evitare
   * problemi di rendering (Flying Saucer non gestisce input interattivi).
   * <p>
   * Preserva attributi {@code value} e {@code placeholder} come contenuto span.
   * </p>
   *
   * @param html HTML originale
   * @return HTML preprocessato
   */
  private static String preprocessHtml(String html)
  {
    String processed;

    processed = html;

    processed = processed.replaceAll(
      "<input[^>]*value=['\"]([^'\"]*)['\"][^>]*>",
      "<span>$1</span>"
    );

    processed = processed.replaceAll(
      "<input[^>]*placeholder=['\"]([^'\"]*)['\"][^>]*>",
      "<span style=\"color: #999;\">$1</span>"
    );

    processed = processed.replaceAll(
      "<input[^>]*>",
      "<span></span>"
    );

    log.debug("HTML preprocessing completed (input tags converted)");
    return processed;
  }

  /**
   * Assicura che l'HTML sia well-formed aggiungendo DOCTYPE e struttura base
   * se mancanti. Flying Saucer richiede XHTML strict con tutti i tag self-closing.
   *
   * @param html HTML da validare
   * @return HTML well-formed XHTML
   */
  private static String ensureWellFormed(String html)
  {
    String processed;
    String trimmed;
    boolean hasDoctype;
    boolean hasHtml;
    boolean hasBody;
    String finalResult;
    StringBuilder wellFormed;

    processed = html;

    processed = processed.replaceAll("<meta([^>]*[^/])>", "<meta$1 />");
    processed = processed.replaceAll("<link([^>]*[^/])>", "<link$1 />");
    processed = processed.replaceAll("<img([^>]*[^/])>", "<img$1 />");
    processed = processed.replaceAll("<br([^>]*[^/])>", "<br$1 />");
    processed = processed.replaceAll("<hr([^>]*[^/])>", "<hr$1 />");
    processed = processed.replaceAll("<input([^>]*[^/])>", "<input$1 />");

    trimmed = processed.trim();
    hasDoctype = trimmed.toLowerCase().startsWith("<!doctype");
    hasHtml = trimmed.toLowerCase().contains("<html");
    hasBody = trimmed.toLowerCase().contains("<body");

    finalResult = null;
    wellFormed = null;

    if (hasDoctype && hasHtml && hasBody) {
      log.debug("HTML already well-formed");
      finalResult = processed;
    } else {
      wellFormed = new StringBuilder();

      if (!hasDoctype) {
        wellFormed.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        wellFormed.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
      }

      if (!hasHtml) {
        wellFormed.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        wellFormed.append("<head><meta charset=\"UTF-8\" /></head>\n");
      }

      if (!hasBody) {
        wellFormed.append("<body>\n");
      }

      wellFormed.append(processed);

      if (!hasBody) {
        wellFormed.append("\n</body>");
      }

      if (!hasHtml) {
        wellFormed.append("\n</html>");
      }

      log.debug("HTML wrapped with well-formed XHTML structure");
      finalResult = wellFormed.toString();
    }
    return finalResult;
  }
}
