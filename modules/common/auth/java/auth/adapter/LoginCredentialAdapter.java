package {{APP_PACKAGE}}.auth.adapter;

import {{APP_PACKAGE}}.auth.dto.LoginCredentialDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza le credenziali di login dal body JSON della richiesta. */
public class LoginCredentialAdapter
{
  /** Legge username e password dal body JSON e li restituisce come DTO. */
  @SuppressWarnings("unchecked")
  public static LoginCredentialDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String password;

    body = Json.decode(req.getBody(), HashMap.class);
    username = Validator.required((String) body.get("username"), "username");
    password = Validator.required((String) body.get("password"), "password");

    return new LoginCredentialDTO(username, password);
  }
}
