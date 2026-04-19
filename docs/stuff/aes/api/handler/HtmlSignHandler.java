package dev.jms.app.module.aes.handler;

import dev.jms.app.module.aes.helper.SignHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HTML2PDF;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import java.util.HashMap;

/**
 * Handler per la conversione HTML → PDF e apposizione campi firma.
 * <p>
 * Flusso:
 * 1. Riceve HTML via body JSON
 * 2. Converte HTML → PDF con preprocessing automatico (tag input → span)
 * 3. Applica campi firma con SignHelper
 * 4. Salva in directory temporanea
 * 5. Restituisce path file e numero placeholder sostituiti
 * </p>
 */
public class HtmlSignHandler
{
  private final SignHelper signHelper;

  public HtmlSignHandler(Config config)
  {
    this.signHelper = new SignHelper(config);
  }

  /**
   * POST /api/aes/firma-html — converte HTML in PDF e applica campi firma.
   * <p>
   * Richiede autenticazione JWT via cookie {@code access_token} o header {@code Authorization: Bearer <token>}.<br>
   * Body JSON: {@code {"html": "..."}}<br>
   * Parametri querystring: {@code placeholder}, {@code width}, {@code height} (in punti PDF).
   * </p>
   * Risposta: {@code replaced} (numero di occorrenze sostituite), {@code outputFile} (path locale).
   */
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String html;
    byte[] pdfBytes;
    String placeholder;
    String widthParam;
    String heightParam;
    SignHelper.Result result;
    HashMap<String, Object> out;

    req.requireAuth();

    body = req.body();
    html = (String) body.get("html");
    placeholder = req.getQueryParam("placeholder");
    widthParam = req.getQueryParam("width");
    heightParam = req.getQueryParam("height");

    if (html == null || html.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro 'html' obbligatorio")
         .out(null)
         .send();
      return;
    }

    if (placeholder == null || placeholder.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro 'placeholder' obbligatorio")
         .out(null)
         .send();
      return;
    }

    if (widthParam == null || heightParam == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametri 'width' e 'height' obbligatori")
         .out(null)
         .send();
      return;
    }

    pdfBytes = HTML2PDF.convert(html, true);

    result = signHelper.sign(
      pdfBytes,
      placeholder,
      Float.parseFloat(widthParam),
      Float.parseFloat(heightParam)
    );

    out = new HashMap<>();
    out.put("replaced", result.replaced);
    out.put("outputFile", result.outputFile);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }
}
