package com.intuit.graphql.orchestrator.batch;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface QueryExecutor {

  CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput, GraphQLContext context);
}
