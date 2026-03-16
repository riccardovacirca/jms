package {{APP_PACKAGE}}.user.handler;

import {{APP_PACKAGE}}.user.adapter.ChangePasswordAdapter;
import {{APP_PACKAGE}}.user.adapter.ForgotPasswordAdapter;
import {{APP_PACKAGE}}.user.adapter.LoginCredentialAdapter;
import {{APP_PACKAGE}}.user.adapter.ResetPasswordAdapter;
import {{APP_PACKAGE}}.user.adapter.TwoFactorCredentialAdapter;
import {{APP_PACKAGE}}.user.dao.AccountDAO;
import {{APP_PACKAGE}}.user.dao.AuthPinDAO;
import {{APP_PACKAGE}}.user.dao.PasswordResetDAO;
import {{APP_PACKAGE}}.user.dao.RefreshTokenDAO;
import {{APP_PACKAGE}}.user.dto.AccountAuthDTO;
import {{APP_PACKAGE}}.user.dto.AuthPinDTO;
import {{APP_PACKAGE}}.user.dto.AuthenticatedAccountDTO;
import {{APP_PACKAGE}}.user.dto.ChangePasswordDTO;
import {{APP_PACKAGE}}.user.dto.ForgotPasswordDTO;
import {{APP_PACKAGE}}.user.dto.LoginCredentialDTO;
import {{APP_PACKAGE}}.user.dto.ResetPasswordDTO;
import {{APP_PACKAGE}}.user.dto.TwoFactorCredentialDTO;
import {{APP_PACKAGE}}.user.helper.LoginHelper;
import {{APP_PACKAGE}}.user.helper.PasswordChangeHelper;
import {{APP_PACKAGE}}.user.helper.PasswordResetHelper;
import {{APP_PACKAGE}}.user.helper.TokenRefreshHelper;
import {{APP_PACKAGE}}.user.helper.TwoFactorHelper;
import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.ValidationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler per le operazioni di autenticazione.
 *
 * <p>Rotte gestite (registrate in Routes.java):
 * <ul>
 *   <li>GET  /api/user/auth/session</li>
 *   <li>GET  /api/user/auth/generate-password</li>
 *   <li>POST /api/user/auth/login</li>
 *   <li>POST /api/user/auth/logout</li>
 *   <li>POST /api/user/auth/refresh</li>
 *   <li>POST /api/user/auth/2fa</li>
 *   <li>POST /api/user/auth/forgot-password</li>
 *   <li>POST /api/user/auth/reset-password</li>
 *   <li>PUT  /api/user/auth/change-password</li>
 * </ul>
 */
public class AuthHandler
{
  private final Config config;

  /** Costruttore con config (usato per app.base.url nel reset password). */
  public AuthHandler(Config config)
  {
    this.config = config;
  }

  /** GET /api/user/auth/session — claims della sessione corrente. Richiede JWT. */
  public void session(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims = req.requireAuth();
    res.status(200).contentType("application/json")
       .err(false).log(null).out(claims).send();
  }

  /** GET /api/user/auth/generate-password — genera password sicura casuale. */
  public void generatePassword(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> out = new HashMap<>();
    out.put("password", Auth.generatePassword());
    res.status(200).contentType("application/json")
       .err(false).log(null).out(out).send();
  }

  /** POST /api/user/auth/login — login con username e password. */
  public void login(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    LoginCredentialDTO creds   = LoginCredentialAdapter.from(req);
    AccountDAO         dao     = new AccountDAO(db);
    AccountAuthDTO     account = dao.findForLogin(creds.username());

    if (account == null || !Auth.checkPassword(creds.password(), account.passwordHash())) {
      res.status(200).contentType("application/json")
         .err(true).log("Credenziali non valide").out(null).send();
      return;
    }
    if (account.twoFactorEnabled()) {
      TwoFactorHelper.issuePin(res, db, account);
      return;
    }
    LoginHelper.issueTokens(res, db, new AuthenticatedAccountDTO(
      account.id(), account.username(), account.ruolo(),
      account.permissions(), account.mustChangePassword()
    ));
  }

  /** POST /api/user/auth/logout — revoca refresh token e cancella cookie. */
  public void logout(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken = req.cookie("refresh_token");
    if (refreshToken != null && !refreshToken.isBlank()) {
      new RefreshTokenDAO(db).delete(refreshToken);
    }
    res.status(200).contentType("application/json")
       .clearCookie("access_token")
       .clearCookie("refresh_token")
       .err(false).log("Logout effettuato").out(null).send();
  }

  /** POST /api/user/auth/refresh — rinnovo access token tramite refresh token. */
  public void refresh(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    TokenRefreshHelper.refresh(req, res, db);
  }

  /** POST /api/user/auth/2fa — verifica PIN two-factor e completa il login. */
  public void twoFactor(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    TwoFactorCredentialDTO creds  = TwoFactorCredentialAdapter.from(req);
    AuthPinDAO             pinDao = new AuthPinDAO(db);
    AuthPinDTO             pin    = pinDao.findByToken(creds.challengeToken());

    if (pin == null
        || LocalDateTime.now().isAfter(pin.expiresAt())
        || !Auth.checkPassword(creds.pin(), pin.pinHash())) {
      res.status(200).contentType("application/json")
         .err(true).log("PIN non valido o scaduto").out(null).send();
      return;
    }
    pinDao.deleteByToken(creds.challengeToken());

    AuthenticatedAccountDTO account = new AccountDAO(db).findById(pin.accountId());
    if (account == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Account non trovato").out(null).send();
      return;
    }
    LoginHelper.issueTokens(res, db, account);
  }

  /** POST /api/user/auth/forgot-password — invia link di reset password via email. */
  public void forgotPassword(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ForgotPasswordDTO dto = ForgotPasswordAdapter.from(req);
    PasswordResetHelper.sendResetLink(db, dto.username(), dto.resetLink());
    res.status(200).contentType("application/json")
       .err(false).log("Se l'account esiste, riceverai un'email con il link di reset").out(null).send();
  }

  /** POST /api/user/auth/reset-password — reset password con token one-time. */
  public void resetPassword(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ResetPasswordDTO  dto      = ResetPasswordAdapter.from(req);
    PasswordResetDAO  resetDao = new PasswordResetDAO(db);
    Integer           accountId = resetDao.findValidAccountId(dto.token());

    if (accountId == null) {
      res.status(200).contentType("application/json")
         .err(true).log("Token non valido o scaduto").out(null).send();
      return;
    }
    new AccountDAO(db).updatePassword(accountId, Auth.hashPassword(dto.password()), false);
    resetDao.markUsed(dto.token());
    res.status(200).contentType("application/json")
       .err(false).log("Password aggiornata").out(null).send();
  }

  /** PUT /api/user/auth/change-password — cambio password autenticato. Richiede JWT. */
  public void changePassword(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> claims    = req.requireAuth();
    long                accountId = Long.parseLong(claims.get("sub").toString());
    ChangePasswordDTO   dto       = ChangePasswordAdapter.from(req);

    try {
      PasswordChangeHelper.changePassword(db, (int) accountId, dto.currentPassword(), dto.newPassword());
    } catch (ValidationException e) {
      res.status(200).contentType("application/json")
         .err(true).log(e.getMessage()).out(null).send();
      return;
    }
    res.status(200).contentType("application/json")
       .err(false).log("Password aggiornata").out(null).send();
  }
}
