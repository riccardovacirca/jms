package dev.crm.module.cloud.config;

import dev.crm.module.cloud.dao.InstallationMetadataDao;
import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.cloud.service.TokenGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CloudConfig
{
  @Bean
  public InstallationMetadataDao installationMetadataDao(DataSource dataSource)
  {
    InstallationMetadataDao dao;

    dao = new InstallationMetadataDao(dataSource);

    return dao;
  }

  @Bean
  public TokenGenerator tokenGenerator()
  {
    TokenGenerator generator;

    generator = new TokenGenerator();

    return generator;
  }

  @Bean
  public InstallationService installationService(
    InstallationMetadataDao dao,
    TokenGenerator tokenGenerator
  )
  {
    InstallationService service;

    service = new InstallationService(dao, tokenGenerator);

    return service;
  }
}
