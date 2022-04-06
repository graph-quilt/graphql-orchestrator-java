package com.intuit.graphql.orchestrator;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VirtualOrchestratorProvider implements ServiceProvider {

  public static final String ORCHESTRATOR = "ORCHESTRATOR";
  public static final String FIELD_NAME = "_namespace";
  public static final String SDL = String.format("type Query { %s : String }", FIELD_NAME);
  public static final String FILE_NAME = String.format("%s.graphqls", ORCHESTRATOR);

  @Override
  public String getNameSpace() {
    return ORCHESTRATOR;
  }

  @Override
  public Map<String, String> sdlFiles() {
    return createData(FILE_NAME, SDL);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(final ExecutionInput executionInput,
      final GraphQLContext context) {
    return CompletableFuture.completedFuture(createData("data",createData(FIELD_NAME, ORCHESTRATOR)));
  }

  private static <T> Map<String, T> createData(String key, T value) {
    final Map<String, T> dataMap = new HashMap<String, T>();
    dataMap.put(key, value);
    return dataMap;
  }
}
