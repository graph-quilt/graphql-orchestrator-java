package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServiceD implements ServiceProvider {

  ServiceD() {
  }

  @Override
  public String getNameSpace() {
    return "SVCD";
  }

  @Override
  public Map<String, String> sdlFiles() {
    String schema = "type Query { } "
        + "type Mutation { container(in : String) : Container } "
        + "type Container { serviceD : ServiceD } "
        + "type ServiceD { svcDField1 : String }";
    return ImmutableMap.of("svcd-schema.graphqls", schema);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    // this is intended to fail during stitching
    // this will not be called
    return null;
  }
}
