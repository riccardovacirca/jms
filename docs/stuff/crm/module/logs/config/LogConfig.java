package dev.crm.module.logs.config;

import dev.crm.module.logs.dao.LogDao;
import dev.crm.module.logs.service.LogService;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig
{
  @Bean
  public LogDao logDao(DataSource dataSource)
  {
    return new LogDao(dataSource);
  }

  @Bean
  public LogService logService(LogDao logDao)
  {
    return new LogService(logDao);
  }
}
