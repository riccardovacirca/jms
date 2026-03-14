package {{APP_PACKAGE}}.auth.handler;

import {{APP_PACKAGE}}.auth.adapter.ForgotPasswordAdapter;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dao.PasswordResetDAO;
import {{APP_PACKAGE}}.auth.dto.AccountAuthDTO;
import {{APP_PACKAGE}}.auth.dto.ForgotPasswordDTO;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Mail;
import dev.jms.util.ValidationException;

import java.security.SecureRandom;
import java.util.HexFormat;

/** Handler per il recupero password. Genera un token monouso e invia un link via email. */
public class ForgotPasswordHandler implements Handler
{
  private static final Log log = Log.get(ForgotPasswordHandler.class);

  /** Genera un token di reset, lo salva nel DB e invia il link all'email dell'account.
   *  Risponde sempre con successo per evitare user enumeration. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ForgotPasswordDTO input;
    AccountAuthDTO account;
    byte[] bytes;
    String token;
    String resetLink;

    try {
      input = ForgotPasswordAdapter.from(req);
      account = new AccountDAO(db).findForLogin(input.username());
      if (account != null && account.email() != null && !account.email().isBlank() && Mail.isConfigured()) {
        bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        token = HexFormat.of().formatHex(bytes);
        resetLink = input.resetLink() + "?token=" + token;
        new PasswordResetDAO(db).saveToken(account.id(), token);
        Mail.get().send(
          account.email(),
          "Recupero password",
          "Clicca il seguente link per reimpostare la tua password:\n\n" + resetLink +
          "\n\nIl link scade tra 1 ora. Se non hai richiesto il recupero, ignora questa email."
        );
        log.info("Link di reset inviato per utente '{}'", input.username());
      } else {
        log.warn("Recupero password: utente '{}' non trovato, senza email o mail non configurata", input.username());
      }
    } catch (ValidationException e) {
      // risposta sempre positiva per evitare user enumeration
    }

    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
