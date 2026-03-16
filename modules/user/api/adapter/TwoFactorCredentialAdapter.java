package {{APP_PACKAGE}}.user.adapter;

import {{APP_PACKAGE}}.user.dto.TwoFactorCredentialDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza le credenziali 2FA (PIN e challenge token) dalla richiesta. */
public class TwoFactorCredentialAdapter
{
  /** Legge il pin dal body JSON e il challenge_token dal cookie. */
  @SuppressWarnings("unchecked")
  public static TwoFactorCredentialDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String challengeToken;
    String pin;

    body           = Json.decode(req.getBody(), HashMap.class);
    challengeToken = Validator.required(req.getCookie("challenge_token"), "challenge_token");
    pin            = Validator.required((String) body.get("pin"), "pin");

    return new TwoFactorCredentialDTO(challengeToken, pin);
  }
}
