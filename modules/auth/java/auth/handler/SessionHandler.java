package com.example.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.auth.dto.AuthenticatedUserDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

public class SessionHandler implements Handler
{
  private static final Log log = Log.get(SessionHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    AuthenticatedUserDTO out;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Sessione rifiutata: cookie access_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
        out = new AuthenticatedUserDTO(
          Integer.parseInt(jwt.getSubject()),
          jwt.getClaim("username").asString(),
          jwt.getClaim("ruolo").asString(),
          jwt.getClaim("can_admin").asBoolean(),
          jwt.getClaim("can_write").asBoolean(),
          jwt.getClaim("can_delete").asBoolean(),
          jwt.getClaim("must_change_password").asBoolean()
        );
        res.status(200).contentType("application/json").err(false).log(null).out(out).send();
      } catch (JWTVerificationException e) {
        log.warn("Sessione rifiutata: token non valido o scaduto");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
