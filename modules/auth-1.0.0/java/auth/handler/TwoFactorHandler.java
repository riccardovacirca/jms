package {{APP_PACKAGE}}.auth.handler;

import {{APP_PACKAGE}}.auth.adapter.TwoFactorCredentialAdapter;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dao.AuthPinDAO;
import {{APP_PACKAGE}}.auth.dto.AuthPinDTO;
import {{APP_PACKAGE}}.auth.dto.AuthenticatedAccountDTO;
import {{APP_PACKAGE}}.auth.dto.TwoFactorCredentialDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

import java.time.LocalDateTime;

/** Handler per la verifica del PIN 2FA. Completa il login dopo la verifica del codice OTP. */
public class TwoFactorHandler implements Handler
{
  private static final Log log = Log.get(TwoFactorHandler.class);

  /** Verifica il PIN e il challenge_token, quindi emette i token di sessione. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    TwoFactorCredentialDTO credential;
    AuthPinDAO authPinDAO;
    AuthPinDTO authPin;
    AuthenticatedAccountDTO account;

    try {
      credential = TwoFactorCredentialAdapter.from(req);
      authPinDAO = new AuthPinDAO(db);
      authPin = authPinDAO.findByToken(credential.challengeToken());
      if (authPin == null) {
        log.warn("2FA fallito: challenge_token non trovato");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Codice non valido o scaduto")
           .out(null)
           .send();
      } else if (authPin.expiresAt() != null && LocalDateTime.now().isAfter(authPin.expiresAt())) {
        authPinDAO.deleteByToken(credential.challengeToken());
        log.warn("2FA fallito: PIN scaduto");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Codice scaduto")
           .out(null)
           .send();
      } else if (!Auth.verifyPassword(credential.pin(), authPin.pinHash())) {
        log.warn("2FA fallito: PIN errato");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Codice non valido")
           .out(null)
           .send();
      } else {
        authPinDAO.deleteByToken(credential.challengeToken());
        account = new AccountDAO(db).findById(authPin.accountId());
        if (account == null) {
          log.warn("2FA: account {} non trovato o disabilitato dopo verifica PIN", authPin.accountId());
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Account non disponibile")
             .out(null)
             .send();
        } else {
          log.info("2FA completato per utente '{}'", account.username());
          LoginHandler.issueTokens(res, db, account);
        }
      }
    } catch (ValidationException e) {
      log.warn("2FA fallito: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }
}
