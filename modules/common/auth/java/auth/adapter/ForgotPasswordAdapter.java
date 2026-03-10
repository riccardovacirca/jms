package {{APP_PACKAGE}}.auth.adapter;

import {{APP_PACKAGE}}.auth.dto.ForgotPasswordDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza lo username per il recupero password dal body JSON della richiesta. */
public class ForgotPasswordAdapter
{
  /** Legge lo username dal body JSON. */
  @SuppressWarnings("unchecked")
  public static ForgotPasswordDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String username;

    body = Json.decode(req.getBody(), HashMap.class);
    username = Validator.required((String) body.get("username"), "username");

    return new ForgotPasswordDTO(username);
  }
}
