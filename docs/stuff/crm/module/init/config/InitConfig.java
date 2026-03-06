package dev.crm.module.init.config;

import dev.crm.module.auth.dao.UtenteDao;
import dev.crm.module.init.dao.AziendaDao;
import dev.crm.module.init.dao.InstallationDao;
import dev.crm.module.init.dao.SedeDao;
import dev.crm.module.init.service.InitService;
import dev.crm.module.logs.service.LogService;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitConfig
{
  @Bean
  public AziendaDao initAziendaDao(DataSource dataSource)
  {
    return new AziendaDao(dataSource);
  }

  @Bean
  public SedeDao initSedeDao(DataSource dataSource)
  {
    return new SedeDao(dataSource);
  }

  @Bean
  public InstallationDao initInstallationDao(DataSource dataSource)
  {
    return new InstallationDao(dataSource);
  }

  @Bean
  public InitService initService(
      AziendaDao initAziendaDao,
      SedeDao initSedeDao,
      UtenteDao utenteDao,
      InstallationDao initInstallationDao,
      PasswordEncoder passwordEncoder,
      LogService logService)
  {
    return new InitService(
        initAziendaDao, initSedeDao, utenteDao, initInstallationDao, passwordEncoder, logService);
  }
}
