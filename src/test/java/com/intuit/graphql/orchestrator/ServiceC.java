package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServiceC implements ServiceProvider {

  ServiceC() {
  }

  @Override
  public String getNameSpace() {
    return "SVCC";
  }

  @Override
  public Map<String, String> sdlFiles() {
    String schema = "type Query { container : Container @deprecated } "
        + "type Container { serviceC : ServiceC } "
        + "type ServiceC { svcCField1 : String }";

    return ImmutableMap.of("svcc-schema.graphqls", schema);
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    // this is intended to fail during stitching
    // this will not be called
    return null;
  }
}
