package com.example.auth.adapter;

import com.example.auth.dto.TwoFactorCredentialDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

public class TwoFactorCredentialAdapter
{
  @SuppressWarnings("unchecked")
  public static TwoFactorCredentialDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String challengeToken;
    String pin;

    body = Json.decode(req.getBody(), HashMap.class);
    challengeToken = Validator.required(req.getCookie("challenge_token"), "challenge_token");
    pin = Validator.required((String) body.get("pin"), "pin");

    return new TwoFactorCredentialDTO(challengeToken, pin);
  }
}
