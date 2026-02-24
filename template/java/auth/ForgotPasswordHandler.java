package {{APP_PACKAGE}}.auth;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.Mail;

import java.util.ArrayList;
import java.util.HashMap;

public class ForgotPasswordHandler implements Handler
{
  private static final Log log = Log.get(ForgotPasswordHandler.class);

  @Override
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> user;
    int userId;
    String email;
    String newPassword;
    String newHash;

    body     = Json.decode(req.getBody(), HashMap.class);
    username = (String) body.get("username");

    if (username != null && !username.isBlank()) {
      sql  = "SELECT id, email FROM users WHERE username = ? AND attivo = true";
      rows = db.select(sql, username);

      if (!rows.isEmpty()) {
        user  = rows.get(0);
        userId = DB.toInteger(user.get("id"));
        email  = DB.toString(user.get("email"));

        if (email != null && !email.isBlank() && Mail.isConfigured()) {
          newPassword = Auth.generatePassword();
          newHash     = Auth.hashPassword(newPassword);

          sql = "UPDATE users SET password_hash = ?, must_change_password = true WHERE id = ?";
          db.query(sql, newHash, userId);

          Mail.get().send(
            email,
            "Recupero password",
            "La tua nuova password temporanea è: " + newPassword +
            "\n\nTi verrà chiesto di cambiarla al prossimo accesso."
          );

          log.info("Password rigenerata e inviata per utente '{}'", username);
        } else {
          log.warn("Recupero password: utente '{}' senza email o mail non configurata", username);
        }
      }
    }

    // Risposta sempre positiva per evitare user enumeration
    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
