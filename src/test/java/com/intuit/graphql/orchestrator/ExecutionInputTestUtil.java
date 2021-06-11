package com.intuit.graphql.orchestrator;

import static com.intuit.graphql.orchestrator.TestHelper.TEST_MAPPER;

import graphql.ExecutionInput;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class ExecutionInputTestUtil {

  public static UnaryOperator<ExecutionInput.Builder> builderFunc(String graphqlQueryStr) throws IOException {
    return  builder -> {
      Map<String, Object> queryMap = null;
      try {
        queryMap = TEST_MAPPER.readValue(graphqlQueryStr, Map.class);
        Objects.requireNonNull(queryMap.get("query"));
        builder.query((String)queryMap.get("query"));
        if (Objects.nonNull(queryMap.get("variables"))) builder.variables((Map<String, Object>)queryMap.get("variables"));
        if (Objects.nonNull(queryMap.get("operationName"))) builder.operationName((String)queryMap.get("operationName"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return builder;
    };
  }
}
