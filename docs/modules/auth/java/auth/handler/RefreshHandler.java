package com.example.auth.handler;

import com.example.auth.dao.RefreshTokenDAO;
import com.example.auth.dao.UserDAO;
import com.example.auth.dto.AuthenticatedUserDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.time.LocalDateTime;

public class RefreshHandler implements Handler
{
  private static final Log log = Log.get(RefreshHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;
    AuthenticatedUserDTO user;
    RefreshTokenDAO refreshTokenDAO;
    String newRefreshToken;
    LocalDateTime expiresAt;

    refreshToken = req.getCookie("refresh_token");
    if (refreshToken == null) {
      log.warn("Refresh rifiutato: cookie refresh_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      user = new UserDAO(db).findByRefreshToken(refreshToken);
      if (user == null) {
        log.warn("Refresh rifiutato: token non trovato, scaduto o utente disabilitato");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        refreshTokenDAO = new RefreshTokenDAO(db);
        newRefreshToken = Auth.generateRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);
        refreshTokenDAO.delete(refreshToken);
        refreshTokenDAO.insert(newRefreshToken, user.id(), expiresAt);
        res.status(200)
           .contentType("application/json")
           .cookie("access_token", Auth.get().createAccessToken(
             user.id(), user.username(), user.ruolo(),
             user.canAdmin(), user.canWrite(), user.canDelete(),
             user.mustChangePassword()
           ), 15 * 60)
           .cookie("refresh_token", newRefreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(null)
           .send();
      }
    }
  }
}
