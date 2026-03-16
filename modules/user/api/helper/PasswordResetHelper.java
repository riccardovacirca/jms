package {{APP_PACKAGE}}.user.helper;

import {{APP_PACKAGE}}.user.dao.AccountDAO;
import {{APP_PACKAGE}}.user.dao.PasswordResetDAO;
import {{APP_PACKAGE}}.user.dto.AccountAuthDTO;
import dev.jms.util.DB;
import dev.jms.util.Log;
import dev.jms.util.Mail;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Helper per la generazione del token di reset e l'invio dell'email.
 * Risponde sempre in modo neutro per evitare user enumeration.
 */
public class PasswordResetHelper
{
  private static final Log log = Log.get(PasswordResetHelper.class);

  /** Genera un token di reset, lo salva nel DB e invia il link all'email dell'account se disponibile. */
  public static void sendResetLink(DB db, String username, String resetLink) throws Exception
  {
    AccountAuthDTO account;
    byte[] bytes;
    String token;
    String fullLink;

    account = new AccountDAO(db).findForLogin(username);
    if (account != null && account.email() != null && !account.email().isBlank() && Mail.isConfigured()) {
      bytes    = new byte[32];
      new SecureRandom().nextBytes(bytes);
      token    = HexFormat.of().formatHex(bytes);
      fullLink = resetLink + "?token=" + token;
      new PasswordResetDAO(db).saveToken(account.id(), token);
      Mail.get().send(
        account.email(),
        "Recupero password",
        "Clicca il seguente link per reimpostare la tua password:\n\n" + fullLink +
        "\n\nIl link scade tra 1 ora. Se non hai richiesto il recupero, ignora questa email."
      );
      log.info("Link di reset inviato per utente '{}'", username);
    } else {
      log.warn("Recupero password: utente '{}' non trovato, senza email o mail non configurata", username);
    }
  }
}
