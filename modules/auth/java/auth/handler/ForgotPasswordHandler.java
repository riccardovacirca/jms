package com.example.auth.handler;

import com.example.auth.adapter.ForgotPasswordAdapter;
import com.example.auth.dao.UserDAO;
import com.example.auth.dto.ForgotPasswordDTO;
import com.example.auth.dto.UserAuthDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Mail;
import dev.jms.util.ValidationException;

public class ForgotPasswordHandler implements Handler
{
  private static final Log log = Log.get(ForgotPasswordHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    ForgotPasswordDTO input;
    UserAuthDTO user;
    String newPassword;
    String newHash;

    try {
      input = ForgotPasswordAdapter.from(req);
      user = new UserDAO(db).findForLogin(input.username());
      if (user != null && user.email() != null && !user.email().isBlank() && Mail.isConfigured()) {
        newPassword = Auth.generatePassword();
        newHash = Auth.hashPassword(newPassword);
        new UserDAO(db).updatePassword(user.id(), newHash, true);
        Mail.get().send(
          user.email(),
          "Recupero password",
          "La tua nuova password temporanea è: " + newPassword +
          "\n\nTi verrà chiesto di cambiarla al prossimo accesso."
        );
        log.info("Password rigenerata e inviata per utente '{}'", input.username());
      } else {
        log.warn("Recupero password: utente '{}' non trovato, senza email o mail non configurata", input.username());
      }
    } catch (ValidationException e) {
      // risposta sempre positiva per evitare user enumeration
    }

    res.status(200).contentType("application/json").err(false).log(null).out(null).send();
  }
}
