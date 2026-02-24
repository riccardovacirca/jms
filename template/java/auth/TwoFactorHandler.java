package {{APP_PACKAGE}}.auth;

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

public class TwoFactorHandler implements Handler
{
  private static final Log log = Log.get(TwoFactorHandler.class);

  @Override
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String challengeToken;
    HashMap<String, Object> body;
    String pin;
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> authPin;
    int userId;
    String pinHash;
    LocalDateTime expiresAt;
    HashMap<String, Object> user;
    String uname;
    String ruolo;
    boolean canAdmin;
    boolean canWrite;
    boolean canDelete;
    boolean mustChangePassword;
    String accessToken;
    String refreshToken;
    LocalDateTime refreshExpiresAt;
    HashMap<String, Object> out;

    challengeToken = req.getCookie("challenge_token");
    body           = Json.decode(req.getBody(), HashMap.class);
    pin            = (String) body.get("pin");

    if (challengeToken == null || pin == null || pin.isBlank()) {
      log.warn("2FA fallito: dati mancanti");
      res.status(200).contentType("application/json").err(true).log("Dati mancanti").out(null).send();
      return;
    }

    sql  = "SELECT id, user_id, pin_hash, expires_at FROM auth_pins WHERE challenge_token = ?";
    rows = db.select(sql, challengeToken);

    if (rows.isEmpty()) {
      log.warn("2FA fallito: challenge_token non trovato");
      res.status(200).contentType("application/json").err(true).log("Codice non valido o scaduto").out(null).send();
      return;
    }

    authPin   = rows.get(0);
    expiresAt = DB.toLocalDateTime(authPin.get("expires_at"));

    if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
      db.query("DELETE FROM auth_pins WHERE challenge_token = ?", challengeToken);
      log.warn("2FA fallito: PIN scaduto");
      res.status(200).contentType("application/json").err(true).log("Codice scaduto").out(null).send();
      return;
    }

    pinHash = DB.toString(authPin.get("pin_hash"));

    if (!Auth.verifyPassword(pin, pinHash)) {
      log.warn("2FA fallito: PIN errato");
      res.status(200).contentType("application/json").err(true).log("Codice non valido").out(null).send();
      return;
    }

    userId = DB.toInteger(authPin.get("user_id"));
    db.query("DELETE FROM auth_pins WHERE challenge_token = ?", challengeToken);

    sql  =
      "SELECT u.id, u.username, u.must_change_password, r.name AS ruolo, " +
      "r.can_admin, r.can_write, r.can_delete " +
      "FROM users u JOIN roles r ON r.name = u.ruolo " +
      "WHERE u.id = ? AND u.attivo = true";
    rows = db.select(sql, userId);

    if (rows.isEmpty()) {
      log.warn("2FA: utente {} non trovato o disabilitato dopo verifica PIN", userId);
      res.status(200).contentType("application/json").err(true).log("Utente non disponibile").out(null).send();
      return;
    }

    user               = rows.get(0);
    uname              = DB.toString(user.get("username"));
    ruolo              = DB.toString(user.get("ruolo"));
    canAdmin           = Boolean.TRUE.equals(user.get("can_admin"));
    canWrite           = Boolean.TRUE.equals(user.get("can_write"));
    canDelete          = Boolean.TRUE.equals(user.get("can_delete"));
    mustChangePassword = Boolean.TRUE.equals(user.get("must_change_password"));

    accessToken       = Auth.get().createAccessToken(userId, uname, ruolo, canAdmin, canWrite, canDelete, mustChangePassword);
    refreshToken      = Auth.generateRefreshToken();
    refreshExpiresAt  = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

    db.query(
      "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
      refreshToken, userId, DB.toSqlTimestamp(refreshExpiresAt)
    );

    log.info("2FA completato per utente '{}'", uname);

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
       .cookie("access_token",    accessToken,  15 * 60)
       .cookie("refresh_token",   refreshToken, Auth.REFRESH_EXPIRY)
       .cookie("challenge_token", "",           0)
       .err(false).log(null).out(out)
       .send();
  }
}
