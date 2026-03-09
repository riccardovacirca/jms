package {{APP_PACKAGE}}.auth.handler;

import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dao.RefreshTokenDAO;
import {{APP_PACKAGE}}.auth.dto.AuthenticatedAccountDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.time.LocalDateTime;

/** Handler per il rinnovo dei token. Ruota il refresh token e ne emette uno nuovo. */
public class RefreshHandler implements Handler
{
  private static final Log log = Log.get(RefreshHandler.class);

  /** Rinnova access token e refresh token a partire dal cookie refresh_token. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;
    AuthenticatedAccountDTO account;
    RefreshTokenDAO refreshTokenDAO;
    String newRefreshToken;
    String newAccessToken;
    LocalDateTime expiresAt;

    refreshToken = req.getCookie("refresh_token");
    if (refreshToken == null) {
      log.warn("Refresh rifiutato: cookie refresh_token assente");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      account = new AccountDAO(db).findByRefreshToken(refreshToken);
      if (account == null) {
        log.warn("Refresh rifiutato: token non trovato, scaduto o account disabilitato");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      } else {
        refreshTokenDAO = new RefreshTokenDAO(db);
        newRefreshToken = Auth.generateRefreshToken();
        newAccessToken = Auth.get().createAccessToken(
          account.id(), account.username(), account.ruolo(),
          account.permissions(), account.mustChangePassword()
        );
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);
        refreshTokenDAO.delete(refreshToken);
        refreshTokenDAO.insert(newRefreshToken, account.id(), expiresAt);
        res.status(200)
           .contentType("application/json")
           .cookie("access_token", newAccessToken, 15 * 60)
           .cookie("refresh_token", newRefreshToken, Auth.REFRESH_EXPIRY)
           .err(false)
           .log(null)
           .out(null)
           .send();
      }
    }
  }
}
