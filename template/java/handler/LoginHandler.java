package {{APP_PACKAGE}}.handler;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

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
    String ruolo;
    boolean canAdmin;
    boolean canWrite;
    boolean canDelete;
    boolean mustChangePassword;
    String accessToken;
    String refreshToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    body = Json.decode(req.getBody(), HashMap.class);
    username = (String) body.get("username");
    password = (String) body.get("password");

    if (username == null || password == null || username.isBlank() || password.isBlank()) {
      log.warn("Login fallito: credenziali mancanti");
      res.status(200).contentType("application/json").err(true).log("Credenziali mancanti").out(null).send();
    } else {
      sql =
        "SELECT u.id, u.username, u.password_hash, u.must_change_password, r.name AS ruolo, " +
        "r.can_admin, r.can_write, r.can_delete " +
        "FROM users u JOIN roles r ON r.name = u.ruolo " +
        "WHERE u.username = ? AND u.attivo = true";
      rows = db.select(sql, username);

      if (rows.isEmpty() || !Auth.verifyPassword(password, (String) rows.get(0).get("password_hash"))) {
        log.warn("Login fallito: credenziali non valide per utente '{}'", username);
        res.status(200).contentType("application/json").err(true).log("Credenziali non valide").out(null).send();
      } else {
        user = rows.get(0);
        userId = DB.toInteger(user.get("id"));
        uname = (String) user.get("username");
        ruolo = (String) user.get("ruolo");
        canAdmin = Boolean.TRUE.equals(user.get("can_admin"));
        canWrite = Boolean.TRUE.equals(user.get("can_write"));
        canDelete = Boolean.TRUE.equals(user.get("can_delete"));
        mustChangePassword = Boolean.TRUE.equals(user.get("must_change_password"));

        accessToken = Auth.get().createAccessToken(userId, uname, ruolo, canAdmin, canWrite, canDelete, mustChangePassword);
        refreshToken = Auth.generateRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

        sql = "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)";
        db.query(sql, refreshToken, userId, DB.toSqlTimestamp(expiresAt));

        out = new HashMap<>();
        out.put("id", userId);
        out.put("username", uname);
        out.put("ruolo", ruolo);
        out.put("can_admin", canAdmin);
        out.put("can_write", canWrite);
        out.put("can_delete", canDelete);
        out.put("must_change_password", mustChangePassword);

        res.status(200)
           .contentType("application/json")
           .cookie("access_token", accessToken, 15 * 60)
           .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(out)
           .send();
      }
    }
  }
}
