package dev.jms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Json {

  private static final ObjectMapper mapper = new ObjectMapper()
    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

  public static String encode(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("JSON encode failed: " + e.getMessage(), e);
    }
  }

  public static <T> T decode(String json, Class<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("JSON decode failed: " + e.getMessage(), e);
    }
  }
}
