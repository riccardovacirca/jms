package com.example.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.auth.adapter.ChangePasswordAdapter;
import com.example.auth.dao.UserDAO;
import com.example.auth.dto.ChangePasswordDTO;
import com.example.auth.dto.UserAuthDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

public class ChangePasswordHandler implements Handler
{
  private static final Log log = Log.get(ChangePasswordHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    ChangePasswordDTO input;
    int userId;
    UserDAO userDAO;
    UserAuthDTO user;
    String newHash;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Cambio password rifiutato: cookie access_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
        try {
          input = ChangePasswordAdapter.from(req);
          userId = Integer.parseInt(jwt.getSubject());
          userDAO = new UserDAO(db);
          user = userDAO.findForPasswordChange(userId);
          if (user == null || !Auth.verifyPassword(input.currentPassword(), user.passwordHash())) {
            log.warn("Cambio password rifiutato: password corrente non valida per userId {}", userId);
            res.status(200).contentType("application/json").err(true).log("Password corrente non valida").out(null).send();
          } else {
            newHash = Auth.hashPassword(input.newPassword());
            userDAO.updatePassword(userId, newHash, false);
            log.info("Password aggiornata per userId {}", userId);
            res.status(200).contentType("application/json").err(false).log(null).out(null).send();
          }
        } catch (ValidationException e) {
          log.warn("Cambio password rifiutato: {}", e.getMessage());
          res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
        }
      } catch (JWTVerificationException e) {
        log.warn("Cambio password rifiutato: token non valido o scaduto");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
