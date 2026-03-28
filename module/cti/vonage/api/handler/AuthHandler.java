package dev.jms.app.module.cti.vonage.handler;

import dev.jms.app.module.cti.vonage.helper.AuthHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Session;
import java.util.HashMap;

/**
 * Handler per l'autenticazione locale al modulo CTI tramite API key.
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
   * POST /api/cti/token — verifica l'API key e restituisce un JWT di accesso CTI.
   * <p>
   * Body JSON: {@code {"apiKey": "..."}}<br>
   * Risposta: {@code {"token": "<JWT>"}}.
   * </p>
   */
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String apiKey;
    String token;
    HashMap<String, Object> out;

    body   = req.body();
    apiKey = (String) body.get("apiKey");

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
