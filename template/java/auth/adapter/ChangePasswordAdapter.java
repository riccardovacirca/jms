package {{APP_PACKAGE}}.auth.adapter;

import {{APP_PACKAGE}}.auth.dto.ChangePasswordDTO;
import dev.jms.util.Auth;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;

public class ChangePasswordAdapter
{
  @SuppressWarnings("unchecked")
  public static ChangePasswordDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String currentPassword;
    String newPassword;
    String policyError;

    body = Json.decode(req.getBody(), HashMap.class);
    currentPassword = Validator.required((String) body.get("current_password"), "current_password");
    newPassword = Validator.required((String) body.get("new_password"), "new_password");

    policyError = Auth.validatePassword(newPassword);
    if (policyError != null) {
      throw new ValidationException(policyError);
    }

    return new ChangePasswordDTO(currentPassword, newPassword);
  }
}
