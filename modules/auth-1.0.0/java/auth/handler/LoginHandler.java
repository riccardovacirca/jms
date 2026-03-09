package {{APP_PACKAGE}}.auth.handler;

import {{APP_PACKAGE}}.auth.adapter.LoginCredentialAdapter;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dao.AuthPinDAO;
import {{APP_PACKAGE}}.auth.dao.RefreshTokenDAO;
import {{APP_PACKAGE}}.auth.dto.AccountAuthDTO;
import {{APP_PACKAGE}}.auth.dto.AuthenticatedAccountDTO;
import {{APP_PACKAGE}}.auth.dto.LoginCredentialDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Mail;
import dev.jms.util.ValidationException;

import java.time.LocalDateTime;
import java.util.HashMap;

/** Handler per il login. Supporta autenticazione diretta e flusso 2FA via PIN email. */
public class LoginHandler implements Handler
{
  private static final Log log = Log.get(LoginHandler.class);

  /** Autentica l'account. Se 2FA abilitato invia PIN via email, altrimenti emette i token. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    LoginCredentialDTO credential;
    AccountAuthDTO account;

    try {
      credential = LoginCredentialAdapter.from(req);
      account = new AccountDAO(db).findForLogin(credential.username());
      if (account == null || !Auth.verifyPassword(credential.password(), account.passwordHash())) {
        log.warn("Login fallito: credenziali non valide per utente '{}'", credential.username());
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Credenziali non valide")
           .out(null)
           .send();
      } else if (Mail.isConfigured() && account.twoFactorEnabled() && account.email() != null && !account.email().isBlank()) {
        issuePin(res, db, account);
      } else {
        issueTokens(res, db, account);
      }
    } catch (ValidationException e) {
      log.warn("Login fallito: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  private void issuePin(HttpResponse res, DB db, AccountAuthDTO account) throws Exception
  {
    String pin;
    String pinHash;
    String challengeToken;
    LocalDateTime expiresAt;
    AuthPinDAO authPinDAO;
    HashMap<String, Object> out;

    pin = Auth.generatePin();
    pinHash = Auth.hashPassword(pin);
    challengeToken = Auth.generateRefreshToken();
    expiresAt = LocalDateTime.now().plusMinutes(10);

    authPinDAO = new AuthPinDAO(db);
    authPinDAO.cleanup(account.id());
    authPinDAO.insert(challengeToken, account.id(), pinHash, expiresAt);

    Mail.get().send(
      account.email(),
      "Codice di accesso",
      "Il tuo codice di accesso è: " + pin + "\n\nValido per 10 minuti."
    );

    log.info("2FA PIN inviato a '{}' per utente '{}'", account.email(), account.username());

    out = new HashMap<>();
    out.put("two_factor_required", true);

    res.status(200)
       .contentType("application/json")
       .cookie("challenge_token", challengeToken, 10 * 60)
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  private void issueTokens(HttpResponse res, DB db, AccountAuthDTO account) throws Exception
  {
    issueTokens(res, db,
      new AuthenticatedAccountDTO(
        account.id(), account.username(), account.ruolo(),
        account.permissions(), account.mustChangePassword()
      )
    );
  }

  /** Emette access token e refresh token per l'account autenticato, imposta i cookie e invia la response. */
  static void issueTokens(HttpResponse res, DB db, AuthenticatedAccountDTO account) throws Exception
  {
    String accessToken;
    String refreshToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    accessToken = Auth.get().createAccessToken(
      account.id(), account.username(), account.ruolo(),
      account.permissions(), account.mustChangePassword()
    );
    refreshToken = Auth.generateRefreshToken();
    expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

    new RefreshTokenDAO(db).insert(refreshToken, account.id(), expiresAt);

    out = new HashMap<>();
    out.put("id", account.id());
    out.put("username", account.username());
    out.put("ruolo", account.ruolo());
    out.put("permissions", account.permissions());
    out.put("must_change_password", account.mustChangePassword());

    res.status(200)
       .contentType("application/json")
       .cookie("access_token", accessToken, 15 * 60)
       .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
       .err(false)
       .log(null)
       .out(out)
       .send();
  }
}
