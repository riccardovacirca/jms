package dev.jms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Json
{
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
  }

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
