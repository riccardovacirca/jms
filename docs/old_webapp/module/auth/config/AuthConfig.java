package dev.crm.module.auth.config;

import dev.crm.module.auth.dao.*;
import dev.crm.module.auth.service.AuthService;
import dev.springtools.util.JwtUtil;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig
{
  @Value("${auth.refreshTokenExpiryDays}")
  private int refreshTokenExpiryDays;

  @Bean
  public UtenteDao utenteDao(DataSource dataSource)
  {
    return new UtenteDao(dataSource);
  }

  @Bean
  public RefreshTokenDao refreshTokenDao(DataSource dataSource)
  {
    return new RefreshTokenDao(dataSource);
  }

  @Bean
  public AuthService authService(UtenteDao utenteDao, RefreshTokenDao refreshTokenDao, JwtUtil jwtUtil)
  {
    return new AuthService(utenteDao, refreshTokenDao, jwtUtil, refreshTokenExpiryDays);
  }
}
