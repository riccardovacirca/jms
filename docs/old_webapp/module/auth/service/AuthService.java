package dev.crm.module.auth.service;

import dev.crm.module.auth.dao.RefreshTokenDao;
import dev.crm.module.auth.dao.UtenteDao;
import dev.crm.module.auth.dto.*;
import dev.springtools.util.JwtUtil;
import dev.springtools.util.PasswordUtil;
import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService
{
  private final UtenteDao utenteDao;
  private final RefreshTokenDao refreshTokenDao;
  private final JwtUtil jwtUtil;
  private final int refreshTokenExpiryDays;

  public AuthService(UtenteDao utenteDao, RefreshTokenDao refreshTokenDao,
                     JwtUtil jwtUtil, int refreshTokenExpiryDays)
  {
    this.utenteDao = utenteDao;
    this.refreshTokenDao = refreshTokenDao;
    this.jwtUtil = jwtUtil;
    this.refreshTokenExpiryDays = refreshTokenExpiryDays;
  }

  public LoginResponseDto login(LoginRequestDto req) throws Exception
  {
    Optional<UtenteDto> userOpt;
    UtenteDto user;
    String accessToken;
    RefreshTokenDto refreshToken;
    LoginResponseDto resp;

    userOpt = utenteDao.findByUsername(req.username);
    if (userOpt.isEmpty()) return null;

    user = userOpt.get();
    if (!user.attivo) return null;
    if (!PasswordUtil.verify(req.password, user.passwordHash)) return null;

    accessToken = jwtUtil.generate(user.id, user.username, user.ruolo);
    refreshToken = refreshTokenDao.create(user.id, refreshTokenExpiryDays);

    resp = new LoginResponseDto();
    resp.accessToken = accessToken;
    resp.refreshToken = refreshToken.token;
    resp.userId = user.id;
    resp.username = user.username;
    resp.ruolo = user.ruolo;
    return resp;
  }

  public LoginResponseDto refresh(String refreshTokenValue) throws Exception
  {
    Optional<RefreshTokenDto> tokenOpt;
    RefreshTokenDto token;
    Optional<UtenteDto> userOpt;
    UtenteDto user;
    String accessToken;
    RefreshTokenDto newRefreshToken;
    LoginResponseDto resp;

    tokenOpt = refreshTokenDao.findByToken(refreshTokenValue);
    if (tokenOpt.isEmpty()) return null;

    token = tokenOpt.get();
    if (token.revoked) return null;
    if (token.expiresAt.isBefore(LocalDateTime.now())) return null;

    userOpt = utenteDao.findById(token.utenteId);
    if (userOpt.isEmpty()) return null;

    user = userOpt.get();
    if (!user.attivo) return null;

    // Rotate token
    refreshTokenDao.revoke(refreshTokenValue);

    accessToken = jwtUtil.generate(user.id, user.username, user.ruolo);
    newRefreshToken = refreshTokenDao.create(user.id, refreshTokenExpiryDays);

    resp = new LoginResponseDto();
    resp.accessToken = accessToken;
    resp.refreshToken = newRefreshToken.token;
    resp.userId = user.id;
    resp.username = user.username;
    resp.ruolo = user.ruolo;
    return resp;
  }

  public void logout(String refreshTokenValue) throws Exception
  {
    Optional<RefreshTokenDto> tokenOpt;

    if (refreshTokenValue == null) return;
    tokenOpt = refreshTokenDao.findByToken(refreshTokenValue);
    if (tokenOpt.isPresent()) {
      refreshTokenDao.revoke(refreshTokenValue);
    }
  }
}
