package dev.crm.module.voice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "voice")
public class VoiceConfig
{
  private String provider;
  private String baseUrl;
  private String applicationId;
  private String privateKey;
  private String token;
  private String fromNumber;
  private String testNumber;
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
