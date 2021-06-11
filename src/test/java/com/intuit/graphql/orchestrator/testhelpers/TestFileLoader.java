package com.intuit.graphql.orchestrator.testhelpers;

import static com.intuit.graphql.orchestrator.testhelpers.JsonTestUtils.MAPPER;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class TestFileLoader {

  public static Map<String, Object> loadJsonAsMap(String jsonResourceFile) throws IOException {
    String json = Resources.toString(Resources.getResource(jsonResourceFile), Charset.defaultCharset());
    return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() { });
  }
}
