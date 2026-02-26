package com.example.auth.adapter;

import com.example.auth.dto.ForgotPasswordDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

public class ForgotPasswordAdapter
{
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
