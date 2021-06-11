package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServiceB implements ServiceProvider {

  ServiceB() {
  }

  @Override
  public String getNameSpace() {
    return "SVCB";
  }

  @Override
  public Map<String, String> sdlFiles() {
    String schema = "type Query { container(in : String) : Container } "
        + "type Container { serviceB : ServiceB } "
        + "type ServiceB { svcBField1 : String }";
    return ImmutableMap.of("svcb-schema.graphqls", schema);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    // this is intended to fail during stitching
    // this will not be called
    return null;
  }
}
