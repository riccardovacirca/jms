package com.example.auth.handler;

import com.example.auth.adapter.LoginCredentialAdapter;
import com.example.auth.dao.AuthPinDAO;
import com.example.auth.dao.RefreshTokenDAO;
import com.example.auth.dao.UserDAO;
import com.example.auth.dto.AuthenticatedUserDTO;
import com.example.auth.dto.LoginCredentialDTO;
import com.example.auth.dto.UserAuthDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Mail;
import dev.jms.util.ValidationException;

import java.time.LocalDateTime;
import java.util.HashMap;

public class LoginHandler implements Handler
{
  private static final Log log = Log.get(LoginHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    LoginCredentialDTO credential;
    UserAuthDTO user;

    try {
      credential = LoginCredentialAdapter.from(req);
      user = new UserDAO(db).findForLogin(credential.username());
      if (user == null || !Auth.verifyPassword(credential.password(), user.passwordHash())) {
        log.warn("Login fallito: credenziali non valide per utente '{}'", credential.username());
        res.status(200)
          .contentType("application/json")
          .err(true).log("Credenziali non valide").out(null)
          .send();
      } else if (Mail.isConfigured() && user.twoFactorEnabled() && user.email() != null && !user.email().isBlank()) {
        issuePin(res, db, user);
      } else {
        issueTokens(res, db, user);
      }
    } catch (ValidationException e) {
      log.warn("Login fallito: {}", e.getMessage());
      res.status(200)
        .contentType("application/json")
        .err(true).log(e.getMessage()).out(null)
        .send();
    }
  }

  private void issuePin(HttpResponse res, DB db, UserAuthDTO user) throws Exception
  {
    String pin;
    String pinHash;
    String challengeToken;
    LocalDateTime expiresAt;
    AuthPinDAO authPinDAO;
    HashMap<String, Object> out;

    pin = Auth.generatePin();
    pinHash = Auth.hashPassword(pin);
    challengeToken = Auth.generateRefreshToken();
    expiresAt = LocalDateTime.now().plusMinutes(10);

    authPinDAO = new AuthPinDAO(db);
    authPinDAO.cleanup(user.id());
    authPinDAO.insert(challengeToken, user.id(), pinHash, expiresAt);

    Mail.get().send(
      user.email(),
      "Codice di accesso",
      "Il tuo codice di accesso è: " + pin + "\n\nValido per 10 minuti."
    );

    log.info("2FA PIN inviato a '{}' per utente '{}'", user.email(), user.username());

    out = new HashMap<>();
    out.put("two_factor_required", true);

    res.status(200)
       .contentType("application/json")
       .cookie("challenge_token", challengeToken, 10 * 60)
       .err(false).log(null).out(out)
       .send();
  }

  private void issueTokens(HttpResponse res, DB db, UserAuthDTO user) throws Exception
  {
    issueTokens(res, db,
      new AuthenticatedUserDTO(
        user.id(), user.username(), user.ruolo(),
        user.canAdmin(), user.canWrite(), user.canDelete(),
        user.mustChangePassword()
      )
    );
  }

  static void issueTokens(HttpResponse res, DB db, AuthenticatedUserDTO user) throws Exception
  {
    String accessToken;
    String refreshToken;
    LocalDateTime expiresAt;
    HashMap<String, Object> out;

    accessToken = Auth.get().createAccessToken(
      user.id(), user.username(), user.ruolo(),
      user.canAdmin(), user.canWrite(), user.canDelete(),
      user.mustChangePassword()
    );
    refreshToken = Auth.generateRefreshToken();
    expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

    new RefreshTokenDAO(db).insert(refreshToken, user.id(), expiresAt);

    out = new HashMap<>();
    out.put("id", user.id());
    out.put("username", user.username());
    out.put("ruolo", user.ruolo());
    out.put("can_admin", user.canAdmin());
    out.put("can_write", user.canWrite());
    out.put("can_delete", user.canDelete());
    out.put("must_change_password", user.mustChangePassword());

    res.status(200)
       .contentType("application/json")
       .cookie("access_token", accessToken, 15 * 60)
       .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
       .err(false).log(null).out(out)
       .send();
  }
}
