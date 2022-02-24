package com.intuit.graphql.orchestrator.batch;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;

/**
 * Provides custom observability hooks in the execution of a batch loader.
 *
 * @param <K> A key type that matches the BatchLoader
 * @param <V> A value type that matches the BatchLoader
 */
public interface BatchLoaderExecutionHooks<K, V> {

  BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> DEFAULT_HOOKS =
      new BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>>() {
      };

  /**
   * This is executed first, before all other work in the batch loader {@code load} function.
   *
   * @param context the instance of {@link GraphQLContext} used in the execution context.
   * @param batchLoaderKeys a list of batch loader keys passed to the data loader {@code load} function.
   */
  default void onBatchLoadStart(GraphQLContext context, List<K> batchLoaderKeys) {

  }

  /**
   * This is executed after an ExecutionInput is created, but before the executionInput is used.
   *
   * @param context the instance of {@link GraphQLContext} used in the execution context.
   * @param executionInput the ExecutionInput bound for a Query function
   */
  default void onExecutionInput(GraphQLContext context, ExecutionInput executionInput) {

  }

  /**
   * This is executed after receiving the result from the batch query.
   *
   * @param context the instance of {@link GraphQLContext} used in the execution context.
   * @param queryResult the query result in GraphQL specification format
   */
  default void onQueryResult(GraphQLContext context, Map<String, Object> queryResult) {

  }

  /**
   * This is executed before the batch loader returns with the results.
   *
   * @param context the instance of {@link GraphQLContext} used in the execution context.
   * @param batchLoaderResults the list of batch loader results that match the batch loader keys
   */
  default void onBatchLoadEnd(GraphQLContext context, List<V> batchLoaderResults) {

  }
}
