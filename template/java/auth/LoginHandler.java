package {{APP_PACKAGE}}.auth;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.Mail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class LoginHandler implements Handler
{
  private static final Log log = Log.get(LoginHandler.class);

  @Override
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String password;
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> user;
    int userId;
    String uname;
    String email;
    String ruolo;
    boolean canAdmin;
    boolean canWrite;
    boolean canDelete;
    boolean mustChangePassword;

    body = Json.decode(req.getBody(), HashMap.class);
    username = (String) body.get("username");
    password = (String) body.get("password");

    if (username == null || password == null || username.isBlank() || password.isBlank()) {
      log.warn("Login fallito: credenziali mancanti");
      res.status(200).contentType("application/json").err(true).log("Credenziali mancanti").out(null).send();
    } else {
      sql =
        "SELECT u.id, u.username, u.password_hash, u.must_change_password, u.email, r.name AS ruolo, " +
        "r.can_admin, r.can_write, r.can_delete " +
        "FROM users u JOIN roles r ON r.name = u.ruolo " +
        "WHERE u.username = ? AND u.attivo = true";
      rows = db.select(sql, username);

      if (rows.isEmpty() || !Auth.verifyPassword(password, (String) rows.get(0).get("password_hash"))) {
        log.warn("Login fallito: credenziali non valide per utente '{}'", username);
        res.status(200).contentType("application/json").err(true).log("Credenziali non valide").out(null).send();
      } else {
        user               = rows.get(0);
        userId             = DB.toInteger(user.get("id"));
        uname              = (String) user.get("username");
        email              = DB.toString(user.get("email"));
        ruolo              = (String) user.get("ruolo");
        canAdmin           = Boolean.TRUE.equals(user.get("can_admin"));
        canWrite           = Boolean.TRUE.equals(user.get("can_write"));
        canDelete          = Boolean.TRUE.equals(user.get("can_delete"));
        mustChangePassword = Boolean.TRUE.equals(user.get("must_change_password"));

        if (Mail.isConfigured() && email != null && !email.isBlank()) {
          issuePin(res, db, userId, uname, email);
        } else {
          issueTokens(res, db, userId, uname, ruolo, canAdmin, canWrite, canDelete, mustChangePassword);
        }
      }
    }
  }

  private void issuePin(HttpResponse res, DB db, int userId, String uname, String email) throws Exception
  {
    String pin;
    String pinHash;
    String challengeToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    pin            = Auth.generatePin();
    pinHash        = Auth.hashPassword(pin);
    challengeToken = Auth.generateRefreshToken();
    expiresAt      = LocalDateTime.now().plusMinutes(10);

    db.query("DELETE FROM auth_pins WHERE user_id = ? OR expires_at < NOW()", userId);
    db.query(
      "INSERT INTO auth_pins (challenge_token, user_id, pin_hash, expires_at) VALUES (?, ?, ?, ?)",
      challengeToken, userId, pinHash, DB.toSqlTimestamp(expiresAt)
    );

    Mail.get().send(
      email,
      "Codice di accesso",
      "Il tuo codice di accesso è: " + pin + "\n\nValido per 10 minuti."
    );

    log.info("2FA PIN inviato a '{}' per utente '{}'", email, uname);

    out = new HashMap<>();
    out.put("two_factor_required", true);

    res.status(200)
       .contentType("application/json")
       .cookie("challenge_token", challengeToken, 10 * 60)
       .err(false).log(null).out(out)
       .send();
  }

  private void issueTokens(HttpResponse res, DB db, int userId, String uname,
                            String ruolo, boolean canAdmin, boolean canWrite,
                            boolean canDelete, boolean mustChangePassword) throws Exception
  {
    String accessToken;
    String refreshToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    accessToken  = Auth.get().createAccessToken(userId, uname, ruolo, canAdmin, canWrite, canDelete, mustChangePassword);
    refreshToken = Auth.generateRefreshToken();
    expiresAt    = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

    db.query(
      "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
      refreshToken, userId, DB.toSqlTimestamp(expiresAt)
    );

    out = new HashMap<>();
    out.put("id",                  userId);
    out.put("username",            uname);
    out.put("ruolo",               ruolo);
    out.put("can_admin",           canAdmin);
    out.put("can_write",           canWrite);
    out.put("can_delete",          canDelete);
    out.put("must_change_password", mustChangePassword);

    res.status(200)
       .contentType("application/json")
       .cookie("access_token",  accessToken,  15 * 60)
       .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
       .err(false).log(null).out(out)
       .send();
  }
}
