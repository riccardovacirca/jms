package {{APP_PACKAGE}}.user.helper;

import {{APP_PACKAGE}}.user.dao.AccountDAO;
import {{APP_PACKAGE}}.user.dao.RefreshTokenDAO;
import {{APP_PACKAGE}}.user.dto.AuthenticatedAccountDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.time.LocalDateTime;

/**
 * Helper per la rotazione del refresh token.
 * Invalida il vecchio token e ne emette uno nuovo insieme a un nuovo access token.
 */
public class TokenRefreshHelper
{
  private static final Log log = Log.get(TokenRefreshHelper.class);

  /** Esegue il refresh: verifica il cookie, ruota i token, scrive la response. */
  public static void refresh(HttpRequest req, HttpResponse res, DB db) throws Exception
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
      res.status(200).contentType("application/json")
         .err(true).log("Non autenticato").out(null).send();
    } else {
      account = new AccountDAO(db).findByRefreshToken(refreshToken);
      if (account == null) {
        log.warn("Refresh rifiutato: token non trovato, scaduto o account disabilitato");
        res.status(200).contentType("application/json")
           .err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        refreshTokenDAO = new RefreshTokenDAO(db);
        newRefreshToken = Auth.generateRefreshToken();
        newAccessToken  = Auth.get().createAccessToken(
          account.id(), account.username(), account.ruolo(),
          account.permissions(), account.mustChangePassword()
        );
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);
        refreshTokenDAO.delete(refreshToken);
        refreshTokenDAO.insert(newRefreshToken, account.id(), expiresAt);
        res.status(200).contentType("application/json")
           .cookie("access_token",  newAccessToken,  15 * 60)
           .cookie("refresh_token", newRefreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(null).send();
      }
    }
  }
}
