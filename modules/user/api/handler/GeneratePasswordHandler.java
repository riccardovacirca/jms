package {{APP_PACKAGE}}.user.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.HashMap;

/** GET /api/users/generate-password — restituisce una password casuale valida per la policy. */
public class GeneratePasswordHandler implements Handler
{
  private static final Log log = Log.get(GeneratePasswordHandler.class);

  /** Genera una password casuale usando Auth.generatePassword() e la restituisce in chiaro. */
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    String password;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Genera password: token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt      = Auth.get().verifyAccessToken(token);
        password = Auth.generatePassword();
        out      = new HashMap<>();
        out.put("password", password);
        res.status(200).contentType("application/json").err(false).log(null).out(out).send();
      } catch (JWTVerificationException e) {
        log.warn("Genera password: token non valido");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
