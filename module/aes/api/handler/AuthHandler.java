package dev.jms.app.aes.handler;

import dev.jms.app.aes.helper.AuthHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import java.util.HashMap;

/**
 * Handler per l'autenticazione al modulo aes tramite API key.
 * Delega la logica di verifica e creazione token ad {@link AuthHelper}.
 */
public class AuthHandler
{
  private final AuthHelper authHelper;

  /**
   * @param config configurazione applicazione
   */
  public AuthHandler(Config config)
  {
    this.authHelper = new AuthHelper(config);
  }

  /**
   * POST /api/aes/token — verifica l'API key e restituisce un JWT di accesso.
   * <p>
   * Body JSON: {@code {"apiKey": "..."}}<br>
   * Risposta: {@code {"token": "<JWT>"}}.
   * </p>
   */
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String apiKey;
    String token;
    HashMap<String, Object> out;

    apiKey = req.getBodyParam("apiKey", String.class);

    if (apiKey == null || apiKey.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro 'apiKey' obbligatorio")
         .out(null)
         .send();
    } else {
      token = authHelper.createToken(apiKey);
      if (token == null) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("API key non valida")
           .out(null)
           .send();
      } else {
        out = new HashMap<>();
        out.put("token", token);
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
