package com.intuit.graphql.orchestrator.testhelpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonTestUtils {

  static final ObjectMapper MAPPER = new ObjectMapper();

  public static String toJson(Object map) {
    try {
      return MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Input map cannot be converted to json", e);
    }
  }

  public static Map<String, Object> jsonToMap(String jsonString) {
    try {
      return MAPPER.readValue(jsonString, Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("json string cannot be converted to Map", e);
    }
  }

}
