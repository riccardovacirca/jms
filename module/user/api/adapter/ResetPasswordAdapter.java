package dev.jms.app.user.adapter;

import dev.jms.app.user.dto.ResetPasswordDTO;
import dev.jms.util.Auth;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza token e nuova password dal body JSON della richiesta di reset. */
public class ResetPasswordAdapter
{
  /** Legge token e new_password dal body JSON, valida la policy sulla nuova password. */
  @SuppressWarnings("unchecked")
  public static ResetPasswordDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String token;
    String password;
    String policyError;

    body        = Json.decode(req.getBody(), HashMap.class);
    token       = Validator.required((String) body.get("token"),        "token");
    password    = Validator.required((String) body.get("new_password"), "new_password");
    policyError = Auth.validatePassword(password);
    if (policyError != null) {
      throw new ValidationException(policyError);
    }

    return new ResetPasswordDTO(token, password);
  }
}
