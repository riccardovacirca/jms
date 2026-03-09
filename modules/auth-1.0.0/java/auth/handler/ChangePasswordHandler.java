package {{APP_PACKAGE}}.auth.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import {{APP_PACKAGE}}.auth.adapter.ChangePasswordAdapter;
import {{APP_PACKAGE}}.auth.dao.AccountDAO;
import {{APP_PACKAGE}}.auth.dto.AccountAuthDTO;
import {{APP_PACKAGE}}.auth.dto.ChangePasswordDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

/** Handler per il cambio password autenticato. Richiede la password corrente per procedere. */
public class ChangePasswordHandler implements Handler
{
  private static final Log log = Log.get(ChangePasswordHandler.class);

  /** Verifica la password corrente e aggiorna l'hash con la nuova password. */
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    ChangePasswordDTO input;
    int accountId;
    AccountDAO accountDAO;
    AccountAuthDTO account;
    String newHash;

    token = req.getCookie("access_token");
    if (token == null) {
      log.warn("Cambio password rifiutato: cookie access_token assente");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
        try {
          input = ChangePasswordAdapter.from(req);
          accountId = Integer.parseInt(jwt.getSubject());
          accountDAO = new AccountDAO(db);
          account = accountDAO.findForPasswordChange(accountId);
          if (account == null || !Auth.verifyPassword(input.currentPassword(), account.passwordHash())) {
            log.warn("Cambio password rifiutato: password corrente non valida per accountId {}", accountId);
            res.status(200)
               .contentType("application/json")
               .err(true)
               .log("Password corrente non valida")
               .out(null)
               .send();
          } else {
            newHash = Auth.hashPassword(input.newPassword());
            accountDAO.updatePassword(accountId, newHash, false);
            log.info("Password aggiornata per accountId {}", accountId);
            res.status(200)
               .contentType("application/json")
               .err(false)
               .log(null)
               .out(null)
               .send();
          }
        } catch (ValidationException e) {
          log.warn("Cambio password rifiutato: {}", e.getMessage());
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log(e.getMessage())
             .out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        log.warn("Cambio password rifiutato: token non valido o scaduto");
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }
}
