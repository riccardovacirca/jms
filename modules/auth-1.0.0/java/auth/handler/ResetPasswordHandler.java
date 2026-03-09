package {{APP_PACKAGE}}.auth.handler;

import {{APP_PACKAGE}}.auth.adapter.ResetPasswordAdapter;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dao.PasswordResetDAO;
import {{APP_PACKAGE}}.auth.dto.ResetPasswordDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

/** Handler per il reset della password tramite token ricevuto via email. */
public class ResetPasswordHandler implements Handler
{
  private static final Log log = Log.get(ResetPasswordHandler.class);

  /** Valida il token, aggiorna la password e marca il token come usato. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ResetPasswordDTO input;
    Integer accountId;
    String newHash;

    try {
      input = ResetPasswordAdapter.from(req);
      accountId = new PasswordResetDAO(db).findValidAccountId(input.token());
      if (accountId == null) {
        log.warn("Reset password: token non valido o scaduto");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
        return;
      }
      newHash = Auth.hashPassword(input.password());
      new AccountDAO(db).updatePassword(accountId, newHash, false);
      new PasswordResetDAO(db).markUsed(input.token());
      log.info("Password reimpostata con successo per accountId {}", accountId);
    } catch (ValidationException e) {
      res.status(200).contentType("application/json").err(true).log(e.getMessage()).out(null).send();
      return;
    }

    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
