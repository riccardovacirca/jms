package {{APP_PACKAGE}}.handler;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class RefreshHandler implements Handler
{
  private static final Log log = Log.get(RefreshHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;
    String sql;
    ArrayList<HashMap<String, Object>> rows;
    HashMap<String, Object> row;
    int userId;
    String username;
    String ruolo;
    boolean canAdmin;
    boolean canWrite;
    boolean canDelete;
    boolean mustChangePassword;
    String newRefreshToken;
    LocalDateTime expiresAt;

    refreshToken = req.getCookie("refresh_token");

    if (refreshToken == null) {
      log.warn("Refresh rifiutato: cookie refresh_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      sql =
        "SELECT u.id, u.username, u.must_change_password, r.name AS ruolo, " +
        "r.can_admin, r.can_write, r.can_delete " +
        "FROM refresh_tokens rt " +
        "JOIN users u ON u.id = rt.user_id " +
        "JOIN roles r ON r.name = u.ruolo " +
        "WHERE rt.token = ? AND rt.expires_at > NOW() AND u.attivo = true";
      rows = db.select(sql, refreshToken);

      if (rows.isEmpty()) {
        log.warn("Refresh rifiutato: token non trovato, scaduto o utente disabilitato");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        row = rows.get(0);
        userId = DB.toInteger(row.get("id"));
        username = (String) row.get("username");
        ruolo = (String) row.get("ruolo");
        canAdmin = Boolean.TRUE.equals(row.get("can_admin"));
        canWrite = Boolean.TRUE.equals(row.get("can_write"));
        canDelete = Boolean.TRUE.equals(row.get("can_delete"));
        mustChangePassword = Boolean.TRUE.equals(row.get("must_change_password"));

        newRefreshToken = Auth.generateRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

        sql = "DELETE FROM refresh_tokens WHERE token = ?";
        db.query(sql, refreshToken);

        sql = "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)";
        db.query(sql, newRefreshToken, userId, DB.toSqlTimestamp(expiresAt));

        res.status(200)
           .contentType("application/json")
           .cookie("access_token", Auth.get().createAccessToken(userId, username, ruolo, canAdmin, canWrite, canDelete, mustChangePassword), 15 * 60)
           .cookie("refresh_token", newRefreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(null)
           .send();
      }
    }
  }
}
