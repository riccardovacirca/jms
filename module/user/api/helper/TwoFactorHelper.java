package dev.jms.app.user.helper;

import dev.jms.app.user.dao.AuthPinDAO;
import dev.jms.app.user.dto.AccountAuthDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Mail;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Helper per la generazione e l'invio del PIN 2FA.
 * Chiamato dal flusso di login quando two_factor_enabled = true.
 */
public class TwoFactorHelper
{
  private static final Log log = Log.get(TwoFactorHelper.class);

  /** Genera un PIN, lo salva nel DB, lo invia via email e risponde con challenge_token. */
  public static void issuePin(HttpResponse res, DB db, AccountAuthDTO account) throws Exception
  {
    String pin;
    String pinHash;
    String challengeToken;
    LocalDateTime expiresAt;
    AuthPinDAO authPinDAO;
    HashMap<String, Object> out;

    pin            = Auth.generatePin();
    pinHash        = Auth.hashPassword(pin);
    challengeToken = Auth.generateRefreshToken();
    expiresAt      = LocalDateTime.now().plusMinutes(10);

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
       .err(false).log(null).out(out)
       .send();
  }
}
