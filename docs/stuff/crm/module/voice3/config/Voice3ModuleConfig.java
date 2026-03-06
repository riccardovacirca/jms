package dev.crm.module.voice3.config;

import dev.crm.module.cloud.service.InstallationService;
import dev.crm.module.voice3.controller.VoiceController3;
import dev.crm.module.voice3.dao.CallDao3;
import dev.crm.module.voice3.service.VoiceService3;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione Spring del modulo voice3.
 * Centralizza la creazione e il wiring di tutti i bean del modulo
 * tramite dependency injection esplicita via costruttore.
 *
 * Il modulo dipende da:
 *   - Voice3Config: configurazione Vonage (letta da application.properties)
 *   - DataSource: connessione al database (fornita da Spring Boot)
 *   - InstallationService: servizio cloud per la gestione dell'installazione
 *     e la generazione dei token HMAC per l'event_url
 */
@Configuration
public class Voice3ModuleConfig
{
  /**
   * DAO per la persistenza delle chiamate nella tabella voice_calls.
   * Riceve il DataSource per le query JDBC dirette.
   */
  @Bean
  public CallDao3 callDao3(DataSource dataSource)
  {
    return new CallDao3(dataSource);
  }

  /**
   * Service principale del modulo voice3.
   * Implementa la logica operator-first del progressive dialer:
   * l'operatore si connette prima tramite WebRTC, poi il sistema
   * chiama il cliente in modo asincrono.
   */
  @Bean
  public VoiceService3 voiceService3(
      Voice3Config config,
      CallDao3 callDao3,
      InstallationService installationService)
  {
    return new VoiceService3(config, callDao3, installationService);
  }

  /**
   * Controller REST del modulo voice3.
   * Espone gli endpoint su /api/voice3/*.
   */
  @Bean
  public VoiceController3 voiceController3(VoiceService3 voiceService3)
  {
    return new VoiceController3(voiceService3);
  }
}
