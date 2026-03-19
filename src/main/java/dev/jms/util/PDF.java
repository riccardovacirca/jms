package dev.jms.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper per la manipolazione di documenti PDF basato su Apache PDFBox.
 * <p>
 * Istanziato tramite {@link #load(InputStream)}; le modifiche vengono
 * persistite con {@link #save(OutputStream)}, che chiude anche il documento.
 * </p>
 */
public class PDF
{
  private final PDDocument doc;

  private PDF(PDDocument doc)
  {
    this.doc = doc;
  }

  /**
   * Carica un documento PDF da un InputStream.
   *
   * @param in stream del documento PDF
   * @return nuova istanza di PDF pronta per le modifiche
   * @throws IOException se il documento non è leggibile
   */
  public static PDF load(InputStream in) throws IOException
  {
    byte[] bytes;
    PDDocument document;
    bytes = in.readAllBytes();
    document = Loader.loadPDF(bytes);
    return new PDF(document);
  }

  /**
   * Cerca la prima occorrenza di {@code placeholder} e la sostituisce con un campo firma
   * AcroForm ({@code /Sig}) dimensionato sull'area del testo trovato.
   * Se il placeholder non è presente il documento non viene modificato.
   *
   * @param placeholder testo da cercare (es. {@code "{{FIRMA}}"})
   * @param fieldName   nome univoco del campo firma da creare
   * @return {@code true} se il placeholder è stato trovato e sostituito
   * @throws IOException se l'elaborazione del PDF fallisce
   */
  public boolean replacePlaceholderWithSignatureField(String placeholder, String fieldName) throws IOException
  {
    PlaceholderFinder finder;
    List<PlaceholderMatch> matches;
    PDAcroForm acroForm;

    finder = new PlaceholderFinder(placeholder);
    finder.setSortByPosition(true);
    finder.getText(doc);
    matches = finder.getMatches();

    if (!matches.isEmpty()) {
      acroForm = ensureAcroForm();
      applySignatureField(acroForm, fieldName, matches.get(0).getRect(), matches.get(0).getPageIndex());
    }

    return !matches.isEmpty();
  }

  /**
   * Cerca tutte le occorrenze di {@code placeholder} nel documento e sostituisce ciascuna
   * con un campo firma AcroForm dimensionato secondo {@code width} e {@code height} specificati.
   * Il campo è posizionato con il bordo sinistro allineato al placeholder e centrato verticalmente.
   * I nomi dei campi sono {@code fieldNamePrefix} per una sola occorrenza,
   * {@code fieldNamePrefix_1}, {@code fieldNamePrefix_2}, … per occorrenze multiple.
   *
   * @param placeholder     testo da cercare
   * @param fieldNamePrefix prefisso del nome del campo firma
   * @param width           larghezza del campo firma in punti PDF
   * @param height          altezza del campo firma in punti PDF
   * @return numero di occorrenze trovate e sostituite
   * @throws IOException se l'elaborazione del PDF fallisce
   */
  public int replaceAllPlaceholdersWithSignatureField(
    String placeholder,
    String fieldNamePrefix,
    float width,
    float height) throws IOException
  {
    PlaceholderFinder finder;
    List<PlaceholderMatch> matches;
    PDAcroForm acroForm;
    int count;

    finder = new PlaceholderFinder(placeholder);
    finder.setSortByPosition(true);
    finder.getText(doc);
    matches = finder.getMatches();
    count = matches.size();

    if (count > 0) {
      acroForm = ensureAcroForm();
      for (int i = 0; i < count; i++) {
        PlaceholderMatch match;
        String fieldName;
        PDRectangle rect;
        match = matches.get(i);
        fieldName = count == 1 ? fieldNamePrefix : fieldNamePrefix + "_" + (i + 1);
        rect = computeRect(match, width, height);
        applySignatureField(acroForm, fieldName, rect, match.getPageIndex());
      }
    }

    return count;
  }

  /**
   * Salva il documento nello stream di output e rilascia le risorse.
   *
   * @param out stream di destinazione
   * @throws IOException se la scrittura fallisce
   */
  public void save(OutputStream out) throws IOException
  {
    doc.save(out);
    doc.close();
  }

  private PDAcroForm ensureAcroForm() throws IOException
  {
    PDAcroForm acroForm;
    acroForm = doc.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      acroForm = new PDAcroForm(doc);
      doc.getDocumentCatalog().setAcroForm(acroForm);
    }
    return acroForm;
  }

  private void applySignatureField(PDAcroForm acroForm, String fieldName, PDRectangle rect, int pageIndex)
    throws IOException
  {
    PDSignatureField sigField;
    PDAnnotationWidget widget;
    PDPage page;
    List<PDAnnotation> annotations;

    sigField = new PDSignatureField(acroForm);
    sigField.setPartialName(fieldName);
    widget = sigField.getWidgets().get(0);
    widget.setRectangle(rect);
    page = doc.getPage(pageIndex);
    widget.setPage(page);
    annotations = page.getAnnotations();
    annotations.add(widget);
    page.setAnnotations(annotations);
    acroForm.getFields().add(sigField);
  }

  private static PDRectangle computeRect(PlaceholderMatch match, float width, float height)
  {
    PDRectangle src;
    float centerY;
    PDRectangle result;

    src = match.getRect();
    centerY = src.getLowerLeftY() + src.getHeight() / 2;
    result = new PDRectangle(src.getLowerLeftX(), centerY - height / 2, width, height);
    return result;
  }

  /**
   * Scansiona un documento alla ricerca di tutte le occorrenze di un testo target,
   * registrandone la posizione (indice di pagina e bounding box in coordinate PDF).
   */
  private static class PlaceholderFinder extends PDFTextStripper
  {
    private final String target;
    private final List<PlaceholderMatch> matches;
    private List<TextPosition> pagePositions;
    private float pageHeight;

    PlaceholderFinder(String target) throws IOException
    {
      this.target = target;
      this.matches = new ArrayList<>();
      this.pagePositions = new ArrayList<>();
      this.pageHeight = 0f;
    }

    @Override
    protected void startPage(PDPage page) throws IOException
    {
      pagePositions = new ArrayList<>();
      pageHeight = page.getMediaBox().getHeight();
      super.startPage(page);
    }

    @Override
    protected void endPage(PDPage page) throws IOException
    {
      searchInPage();
      super.endPage(page);
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) throws IOException
    {
      pagePositions.addAll(positions);
      super.writeString(text, positions);
    }

    private void searchInPage()
    {
      StringBuilder sb;
      String pageText;
      int idx;
      int searchFrom;
      List<TextPosition> matchPos;
      TextPosition first;
      TextPosition last;
      float minX;
      float maxX;
      float lly;
      float ury;
      PDRectangle rect;

      sb = new StringBuilder();
      for (TextPosition tp : pagePositions) {
        sb.append(tp.getUnicode());
      }
      pageText = sb.toString();
      searchFrom = 0;
      idx = pageText.indexOf(target, searchFrom);

      while (idx >= 0) {
        matchPos = pagePositions.subList(idx, idx + target.length());
        first = matchPos.get(0);
        last = matchPos.get(matchPos.size() - 1);
        minX = first.getX();
        maxX = last.getX() + last.getWidth();
        // tp.getY() è la baseline in coordinate Java2D (origine in alto, y verso il basso).
        // Conversione a coordinate PDF (origine in basso, y verso l'alto):
        // lly ≈ pageHeight - baseline - 20% altezza (margine per discendenti)
        // ury ≈ pageHeight - baseline + 80% altezza (ascendenti e maiuscole)
        lly = pageHeight - first.getY() - first.getHeight() * 0.2f;
        ury = pageHeight - first.getY() + first.getHeight() * 0.8f;
        rect = new PDRectangle(minX, lly, maxX - minX, ury - lly);
        matches.add(new PlaceholderMatch(getCurrentPageNo() - 1, rect));
        searchFrom = idx + target.length();
        idx = pageText.indexOf(target, searchFrom);
      }
    }

    /**
     * Restituisce tutte le occorrenze trovate nel documento.
     */
    List<PlaceholderMatch> getMatches()
    {
      return matches;
    }
  }

  /**
   * Posizione di un testo trovato: indice di pagina (0-based) e bounding box
   * in coordinate PDF (punti, origine in basso a sinistra).
   */
  private static class PlaceholderMatch
  {
    private final int pageIndex;
    private final PDRectangle rect;

    PlaceholderMatch(int pageIndex, PDRectangle rect)
    {
      this.pageIndex = pageIndex;
      this.rect = rect;
    }

    int getPageIndex()
    {
      return pageIndex;
    }

    PDRectangle getRect()
    {
      return rect;
    }
  }
}
