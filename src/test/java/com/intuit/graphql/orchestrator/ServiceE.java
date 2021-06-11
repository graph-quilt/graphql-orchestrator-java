package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServiceE implements ServiceProvider {

  ServiceE() {
  }

  @Override
  public String getNameSpace() {
    return "SVCE";
  }

  @Override
  public Map<String, String> sdlFiles() {
    String schema = "type Query { } "
        + "type Mutation { container(in : String) : Container } "
        + "type Container { serviceE : ServiceE } "
        + "type ServiceE { svcEField1 : String }";

    return ImmutableMap.of("svce-schema.graphqls", schema);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    // this is intended to fail during stitching
    // this will not be called
    return null;
  }
}
