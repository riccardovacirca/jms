package dev.jms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility per la serializzazione e deserializzazione JSON tramite Jackson.
 */
public class Json
{
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
  }

  /**
   * Serializza un oggetto Java in stringa JSON.
   *
   * @param obj oggetto da serializzare
   * @return stringa JSON
   * @throws RuntimeException se la serializzazione fallisce
   */
  public static String encode(Object obj)
  {
    String result;
    try {
      result = mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("JSON encode failed: " + e.getMessage(), e);
    }
    return result;
  }

  /**
   * Deserializza una stringa JSON nel tipo specificato.
   *
   * @param json stringa JSON da deserializzare
   * @param type classe di destinazione
   * @param <T>  tipo generico di ritorno
   * @return istanza del tipo specificato
   * @throws RuntimeException se la deserializzazione fallisce
   */
  public static <T> T decode(String json, Class<T> type)
  {
    T result;
    try {
      result = mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("JSON decode failed: " + e.getMessage(), e);
    }
    return result;
  }
}
