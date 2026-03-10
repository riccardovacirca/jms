package {{APP_PACKAGE}}.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.auth.dto.AuthenticatedAccountDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.List;

/** Handler per la verifica della sessione corrente. Decodifica il JWT e restituisce i dati account. */
public class SessionHandler implements Handler
{
  private static final Log log = Log.get(SessionHandler.class);

  /** Verifica il cookie access_token e restituisce i dati dell'account autenticato. */
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    List<String> permissions;
    AuthenticatedAccountDTO out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Sessione rifiutata: cookie access_token assente");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
        permissions = jwt.getClaim("permissions").asList(String.class);
        out = new AuthenticatedAccountDTO(
          Integer.parseInt(jwt.getSubject()),
          jwt.getClaim("username").asString(),
          jwt.getClaim("ruolo").asString(),
          permissions != null ? permissions : List.of(),
          jwt.getClaim("must_change_password").asBoolean()
        );
        res.status(200)
           .contentType("application/json")
           .err(false)
           .log(null)
           .out(out)
           .send();
      } catch (JWTVerificationException e) {
        log.warn("Sessione rifiutata: token non valido o scaduto");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }
}
