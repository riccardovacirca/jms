package {{APP_PACKAGE}}.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.HashMap;

public class SessionHandler implements Handler
{
  private static final Log log = Log.get(SessionHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    HashMap<String, Object> out;

    token = req.getCookie("access_token");
    jwt = null;

    if (token == null) {
      log.warn("Sessione rifiutata: cookie access_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
      } catch (JWTVerificationException e) {
        log.warn("Sessione rifiutata: token non valido o scaduto");
      }

      if (jwt == null) {
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        out = new HashMap<>();
        out.put("id", jwt.getSubject());
        out.put("username", jwt.getClaim("username").asString());
        out.put("ruolo", jwt.getClaim("ruolo").asString());
        out.put("can_admin", jwt.getClaim("can_admin").asBoolean());
        out.put("can_write", jwt.getClaim("can_write").asBoolean());
        out.put("can_delete", jwt.getClaim("can_delete").asBoolean());
        out.put("must_change_password", jwt.getClaim("must_change_password").asBoolean());

        res.status(200).contentType("application/json").err(false).log(null).out(out).send();
      }
    }
  }
}
