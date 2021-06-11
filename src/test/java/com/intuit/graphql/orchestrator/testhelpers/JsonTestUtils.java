package com.intuit.graphql.orchestrator.testhelpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonTestUtils {

  static final ObjectMapper MAPPER = new ObjectMapper();

  public static String toJson(Object map) {
    try {
      return MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Input map cannot be converted to json", e);
    }
  }

}
