package {{APP_PACKAGE}}.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class ChangePasswordHandler implements Handler
{
  private static final Log log = Log.get(ChangePasswordHandler.class);

  @Override
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;
    HashMap<String, Object> body;
    String currentPassword;
    String newPassword;
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    int userId;
    String newHash;

    token = req.getCookie("access_token");
    jwt = null;

    if (token == null) {
      log.warn("Cambio password rifiutato: cookie access_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      try {
        jwt = Auth.get().verifyAccessToken(token);
      } catch (JWTVerificationException e) {
        log.warn("Cambio password rifiutato: token non valido o scaduto");
      }

      if (jwt == null) {
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        body = Json.decode(req.getBody(), HashMap.class);
        currentPassword = (String) body.get("current_password");
        newPassword = (String) body.get("new_password");

        if (currentPassword == null || newPassword == null || currentPassword.isBlank() || newPassword.isBlank()) {
          log.warn("Cambio password rifiutato: campi mancanti");
          res.status(200).contentType("application/json").err(true).log("Campi obbligatori mancanti").out(null).send();
        } else {
          userId = Integer.parseInt(jwt.getSubject());
          sql = "SELECT password_hash FROM users WHERE id = ? AND attivo = true";
          rows = db.select(sql, userId);

          if (rows.isEmpty() || !Auth.verifyPassword(currentPassword, (String) rows.get(0).get("password_hash"))) {
            log.warn("Cambio password rifiutato: password corrente non valida per userId {}", userId);
            res.status(200).contentType("application/json").err(true).log("Password corrente non valida").out(null).send();
          } else {
            newHash = Auth.hashPassword(newPassword);
            sql = "UPDATE users SET password_hash = ?, must_change_password = false WHERE id = ?";
            db.query(sql, newHash, userId);

            log.info("Password aggiornata per userId {}", userId);
            res.status(200).contentType("application/json").err(false).log(null).out(null).send();
          }
        }
      }
    }
  }
}
