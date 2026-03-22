package dev.jms.app.aes.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.aes.helper.SignHelper;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import java.util.HashMap;

/**
 * Handler per l'apposizione di campi firma AcroForm in documenti PDF.
 * Delega la logica di elaborazione a {@link SignHelper}.
 */
public class SignHandler
{
  private final SignHelper signHelper;

  /** Crea un'istanza con il proprio helper di elaborazione PDF. */
  public SignHandler()
  {
    this.signHelper = new SignHelper();
  }

  /**
   * POST /api/aes/firma — riceve un PDF via multipart e applica un campo firma
   * in corrispondenza di ogni occorrenza del placeholder indicato.
   * <p>
   * Richiede autenticazione JWT via header {@code Authorization: Bearer <token>}
   * oppure cookie {@code access_token}.<br>
   * Parametri multipart: {@code file} (PDF).<br>
   * Parametri querystring: {@code placeholder}, {@code width}, {@code height} (in punti PDF).
   * </p>
   * Risposta: {@code replaced} (numero di occorrenze sostituite), {@code outputFile} (path locale).
   */
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String authHeader;
    String token;
    boolean authenticated;
    byte[] fileBytes;
    String placeholder;
    String widthParam;
    String heightParam;
    SignHelper.Result result;
    HashMap<String, Object> out;

    authHeader = req.getHeader("Authorization");
    token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      token = authHeader.substring(7);
    }
    if (token == null || token.isBlank()) {
      token = req.getCookieValue("access_token");
    }

    authenticated = false;
    try {
      if (token != null && !token.isBlank()) {
        Auth.get().verifyAccessToken(token);
        authenticated = true;
      }
    } catch (JWTVerificationException e) {
      authenticated = false;
    }

    if (!authenticated) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      fileBytes = req.getMultipartFileBytes("file");
      placeholder = req.getQueryParam("placeholder");
      widthParam = req.getQueryParam("width");
      heightParam = req.getQueryParam("height");

      if (fileBytes == null || fileBytes.length == 0) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Nessun file ricevuto")
           .out(null)
           .send();
      } else if (placeholder == null || placeholder.isBlank()) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Parametro 'placeholder' obbligatorio")
           .out(null)
           .send();
      } else if (widthParam == null || heightParam == null) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Parametri 'width' e 'height' obbligatori")
           .out(null)
           .send();
      } else {
        result = signHelper.sign(
          fileBytes,
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
  }
}
