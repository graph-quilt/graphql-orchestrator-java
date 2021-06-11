package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServiceA implements ServiceProvider {

  ServiceA() {
  }

  @Override
  public String getNameSpace() {
    return "SVCA";
  }

  @Override
  public Map<String, String> sdlFiles() {
    String schema = "type Query { container : Container } "
        + "type Container { serviceA : ServiceA } "
        + "type ServiceA { svcAField1 : String }";
    return ImmutableMap.of("svca-schema.graphqls", schema);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    // this is intended to fail during stitching
    // this will not be called
    return null;
  }
}
