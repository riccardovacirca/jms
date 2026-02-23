package dev.crm.module.auth.controller;

import dev.crm.module.auth.dto.*;
import dev.crm.module.auth.service.AuthService;
import dev.springtools.util.CookieUtil;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{
  private final AuthService authService;

  @Value("${auth.cookie.maxAge}")
  private int cookieMaxAge;

  @Value("${auth.cookie.refreshMaxAge}")
  private int refreshCookieMaxAge;

  public AuthController(AuthService authService)
  {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiPayload login(
      @RequestBody LoginRequestDto req,
      HttpServletResponse httpResponse)
  {
    LoginResponseDto resp;
    ApiPayload response;

    try {
      resp = authService.login(req);
      if (resp == null) {
        response = ApiResponse.create()
          .err(true)
          .log("Invalid credentials")
          .status(200)
          .contentType("application/json")
          .build();
        return response;
      }

      CookieUtil.set(httpResponse, "access_token", resp.accessToken, cookieMaxAge);
      CookieUtil.set(httpResponse, "refresh_token", resp.refreshToken, refreshCookieMaxAge);

      response = ApiResponse.create()
        .out(Map.of("userId", resp.userId, "username", resp.username, "ruolo", resp.ruolo))
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
        .err(true)
        .log("Login failed: " + e.getMessage())
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    }
  }

  @PostMapping("/refresh")
  public ApiPayload refresh(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse)
  {
    Optional<String> tokenOpt;
    LoginResponseDto resp;
    ApiPayload response;

    tokenOpt = CookieUtil.get(httpRequest, "refresh_token");
    if (tokenOpt.isEmpty()) {
      response = ApiResponse.create()
        .err(true)
        .log("Missing refresh token")
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    }

    try {
      resp = authService.refresh(tokenOpt.get());
      if (resp == null) {
        response = ApiResponse.create()
          .err(true)
          .log("Invalid refresh token")
          .status(200)
          .contentType("application/json")
          .build();
        return response;
      }

      CookieUtil.set(httpResponse, "access_token", resp.accessToken, cookieMaxAge);
      CookieUtil.set(httpResponse, "refresh_token", resp.refreshToken, refreshCookieMaxAge);

      response = ApiResponse.create()
        .out(Map.of("userId", resp.userId, "username", resp.username, "ruolo", resp.ruolo))
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    } catch (Exception e) {
      response = ApiResponse.create()
        .err(true)
        .log("Refresh failed: " + e.getMessage())
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    }
  }

  @PostMapping("/logout")
  public ApiPayload logout(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse)
  {
    Optional<String> tokenOpt;
    ApiPayload response;

    tokenOpt = CookieUtil.get(httpRequest, "refresh_token");

    try {
      if (tokenOpt.isPresent()) {
        authService.logout(tokenOpt.get());
      }
    } catch (Exception e) {
      // Continue to delete cookies even if logout fails
    }

    CookieUtil.delete(httpResponse, "access_token");
    CookieUtil.delete(httpResponse, "refresh_token");

    response = ApiResponse.create()
      .out(Map.of("success", true))
      .status(200)
      .contentType("application/json")
      .build();
    return response;
  }

  @GetMapping("/session")
  public ApiPayload session(HttpServletRequest httpRequest)
  {
    Long userId;
    String ruolo;
    ApiPayload response;

    userId = (Long) httpRequest.getAttribute("userId");
    ruolo = (String) httpRequest.getAttribute("ruolo");

    if (userId == null) {
      response = ApiResponse.create()
        .err(true)
        .log("No active session")
        .status(200)
        .contentType("application/json")
        .build();
      return response;
    }

    response = ApiResponse.create()
      .out(Map.of("userId", userId, "ruolo", ruolo))
      .status(200)
      .contentType("application/json")
      .build();
    return response;
  }
}
