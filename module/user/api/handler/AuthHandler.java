package dev.jms.app.user.handler;

import dev.jms.app.user.adapter.ChangePasswordAdapter;
import dev.jms.app.user.adapter.ForgotPasswordAdapter;
import dev.jms.app.user.adapter.LoginCredentialAdapter;
import dev.jms.app.user.adapter.ResetPasswordAdapter;
import dev.jms.app.user.adapter.TwoFactorCredentialAdapter;
import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.dao.AuthPinDAO;
import dev.jms.app.user.dao.PasswordResetDAO;
import dev.jms.app.user.dao.RefreshTokenDAO;
import dev.jms.app.user.dto.AccountAuthDTO;
import dev.jms.app.user.dto.AuthPinDTO;
import dev.jms.app.user.dto.AuthenticatedAccountDTO;
import dev.jms.app.user.dto.ChangePasswordDTO;
import dev.jms.app.user.dto.ForgotPasswordDTO;
import dev.jms.app.user.dto.LoginCredentialDTO;
import dev.jms.app.user.dto.ResetPasswordDTO;
import dev.jms.app.user.dto.TwoFactorCredentialDTO;
import dev.jms.app.user.helper.LoginHelper;
import dev.jms.app.user.helper.PasswordChangeHelper;
import dev.jms.app.user.helper.PasswordResetHelper;
import dev.jms.app.user.helper.TokenRefreshHelper;
import dev.jms.app.user.helper.TwoFactorHelper;
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
    AccountAuthDTO     account;
    String             clientIP;
    String             rateLimitKey;

    clientIP      = req.getClientIP();
    rateLimitKey  = "user.login:" + clientIP;

    if (dev.jms.util.RateLimiter.isBlocked(rateLimitKey)) {
      res.status(429).contentType("application/json")
         .err(true).log("Troppi tentativi. Riprova tra qualche minuto.").out(null).send();
      return;
    }

    account = dao.findForLogin(creds.username());

    if (account == null || !Auth.checkPassword(creds.password(), account.passwordHash())) {
      dev.jms.util.RateLimiter.recordFailure(rateLimitKey);
      res.status(200).contentType("application/json")
         .err(true).log("Credenziali non valide").out(null).send();
      return;
    }

    dev.jms.util.RateLimiter.reset(rateLimitKey);

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
    String refreshToken;
    String accessToken;
    String jti;
    long expiresAt;

    refreshToken = req.cookie("refresh_token");
    if (refreshToken != null && !refreshToken.isBlank()) {
      new RefreshTokenDAO(db).delete(refreshToken);
    }

    accessToken = req.cookie("access_token");
    if (accessToken != null && !accessToken.isBlank()) {
      jti       = Auth.get().extractJTI(accessToken);
      expiresAt = Auth.get().extractExpiration(accessToken);
      if (jti != null && expiresAt > 0) {
        dev.jms.util.JWTBlacklist.revoke(jti, expiresAt);
      }
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
    AuthPinDTO             pin;
    String                 clientIP;
    String                 rateLimitKey;
    AuthenticatedAccountDTO account;

    clientIP     = req.getClientIP();
    rateLimitKey = "user.2fa:" + clientIP;

    if (dev.jms.util.RateLimiter.isBlocked(rateLimitKey)) {
      res.status(429).contentType("application/json")
         .err(true).log("Troppi tentativi. Riprova tra qualche minuto.").out(null).send();
      return;
    }

    pin = pinDao.findByToken(creds.challengeToken());

    if (pin == null
        || LocalDateTime.now().isAfter(pin.expiresAt())
        || !Auth.checkPassword(creds.pin(), pin.pinHash())) {
      dev.jms.util.RateLimiter.recordFailure(rateLimitKey);
      res.status(200).contentType("application/json")
         .err(true).log("PIN non valido o scaduto").out(null).send();
      return;
    }

    dev.jms.util.RateLimiter.reset(rateLimitKey);
    pinDao.deleteByToken(creds.challengeToken());

    account = new AccountDAO(db).findById(pin.accountId());
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
    ForgotPasswordDTO dto;
    String            clientIP;
    String            rateLimitKey;

    clientIP     = req.getClientIP();
    rateLimitKey = "user.forgot:" + clientIP;

    if (dev.jms.util.RateLimiter.isBlocked(rateLimitKey)) {
      res.status(429).contentType("application/json")
         .err(true).log("Troppi tentativi. Riprova tra qualche minuto.").out(null).send();
      return;
    }

    dto = ForgotPasswordAdapter.from(req);
    dev.jms.util.RateLimiter.recordFailure(rateLimitKey);
    PasswordResetHelper.sendResetLink(db, dto.username(), dto.resetLink());
    res.status(200).contentType("application/json")
       .err(false).log("Se l'account esiste, riceverai un'email con il link di reset").out(null).send();
  }

  /** POST /api/user/auth/reset-password — reset password con token one-time. */
  public void resetPassword(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ResetPasswordDTO  dto;
    PasswordResetDAO  resetDao;
    Integer           accountId;
    String            clientIP;
    String            rateLimitKey;

    clientIP     = req.getClientIP();
    rateLimitKey = "user.reset:" + clientIP;

    if (dev.jms.util.RateLimiter.isBlocked(rateLimitKey)) {
      res.status(429).contentType("application/json")
         .err(true).log("Troppi tentativi. Riprova tra qualche minuto.").out(null).send();
      return;
    }

    dto      = ResetPasswordAdapter.from(req);
    resetDao = new PasswordResetDAO(db);
    accountId = resetDao.findValidAccountId(dto.token());

    if (accountId == null) {
      dev.jms.util.RateLimiter.recordFailure(rateLimitKey);
      res.status(200).contentType("application/json")
         .err(true).log("Token non valido o scaduto").out(null).send();
      return;
    }

    dev.jms.util.RateLimiter.reset(rateLimitKey);
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
