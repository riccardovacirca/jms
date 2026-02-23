package dev.crm.module.voice3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione del modulo voice3.
 * Le proprietà vengono lette da application.properties con prefisso "voice",
 * le stesse utilizzate dai moduli voice e voice2 per evitare duplicazioni
 * nella configurazione.
 *
 * Proprietà attese in application.properties:
 *   voice.provider         = vonage
 *   voice.base-url         = https://api.nexmo.com/v1/calls
 *   voice.application-id   = <VONAGE_APPLICATION_ID>
 *   voice.private-key      = <path o contenuto PEM>
 *   voice.token            = (opzionale, se vuoto viene generato JWT dinamico)
 *   voice.from-number      = <numero mittente>
 *   voice.test-number      = <numero di test>
 *   voice.event-url        = <URL base per i webhook Vonage>
 */
@Configuration
@ConfigurationProperties(prefix = "voice")
public class Voice3Config
{
  // Provider VoIP (attualmente solo "vonage")
  private String provider;

  // URL base delle Vonage Voice API (es. https://api.nexmo.com/v1/calls)
  private String baseUrl;

  // ID dell'applicazione Vonage
  private String applicationId;

  // Path al file private.key oppure contenuto PEM inline
  private String privateKey;

  // Token statico opzionale; se assente viene generato un JWT RS256 dinamico
  private String token;

  // Numero telefonico mittente registrato su Vonage
  private String fromNumber;

  // Numero di test per le chiamate di verifica
  private String testNumber;

  // URL base a cui Vonage invia gli eventi webhook (event_url)
  private String eventUrl;

  public String getProvider()
  {
    return provider;
  }

  public void setProvider(String provider)
  {
    this.provider = provider;
  }

  public String getBaseUrl()
  {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl)
  {
    this.baseUrl = baseUrl;
  }

  public String getApplicationId()
  {
    return applicationId;
  }

  public void setApplicationId(String applicationId)
  {
    this.applicationId = applicationId;
  }

  public String getPrivateKey()
  {
    return privateKey;
  }

  public void setPrivateKey(String privateKey)
  {
    this.privateKey = privateKey;
  }

  public String getToken()
  {
    return token;
  }

  public void setToken(String token)
  {
    this.token = token;
  }

  public String getFromNumber()
  {
    return fromNumber;
  }

  public void setFromNumber(String fromNumber)
  {
    this.fromNumber = fromNumber;
  }

  public String getTestNumber()
  {
    return testNumber;
  }

  public void setTestNumber(String testNumber)
  {
    this.testNumber = testNumber;
  }

  public String getEventUrl()
  {
    return eventUrl;
  }

  public void setEventUrl(String eventUrl)
  {
    this.eventUrl = eventUrl;
  }
}
