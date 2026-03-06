package com.example.auth.adapter;

import com.example.auth.dto.LoginCredentialDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

public class LoginCredentialAdapter
{
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
