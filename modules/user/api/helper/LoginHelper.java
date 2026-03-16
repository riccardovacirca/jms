package {{APP_PACKAGE}}.user.helper;

import {{APP_PACKAGE}}.user.dao.RefreshTokenDAO;
import {{APP_PACKAGE}}.user.dto.AuthenticatedAccountDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpResponse;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Helper per l'emissione dei token di sessione al termine del login.
 * Genera access token e refresh token, li persiste e imposta i cookie.
 */
public class LoginHelper
{
  /** Costruisce e invia la response di login con i cookie di sessione. */
  public static void issueTokens(HttpResponse res, DB db, AuthenticatedAccountDTO account) throws Exception
  {
    String accessToken;
    String refreshToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    accessToken  = Auth.get().createAccessToken(
      account.id(), account.username(), account.ruolo(),
      account.permissions(), account.mustChangePassword()
    );
    refreshToken = Auth.generateRefreshToken();
    expiresAt    = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

    new RefreshTokenDAO(db).insert(refreshToken, account.id(), expiresAt);

    out = new HashMap<>();
    out.put("id",                  account.id());
    out.put("username",            account.username());
    out.put("ruolo",               account.ruolo());
    out.put("permissions",         account.permissions());
    out.put("must_change_password", account.mustChangePassword());

    res.status(200)
       .contentType("application/json")
       .cookie("access_token",  accessToken,  15 * 60)
       .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
       .err(false).log(null).out(out)
       .send();
  }
}
