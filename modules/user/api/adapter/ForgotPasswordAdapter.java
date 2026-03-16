package {{APP_PACKAGE}}.user.adapter;

import {{APP_PACKAGE}}.user.dto.ForgotPasswordDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza username e reset_link per il recupero password dal body JSON della richiesta. */
public class ForgotPasswordAdapter
{
  /** Legge username e reset_link dal body JSON. */
  @SuppressWarnings("unchecked")
  public static ForgotPasswordDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String resetLink;

    body      = Json.decode(req.getBody(), HashMap.class);
    username  = Validator.required((String) body.get("username"),   "username");
    resetLink = Validator.required((String) body.get("reset_link"), "reset_link");

    return new ForgotPasswordDTO(username, resetLink);
  }
}
