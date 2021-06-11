package com.intuit.graphql.orchestrator;

import static java.util.Objects.requireNonNull;

import com.intuit.graphql.orchestrator.schema.RuntimeGraph;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.ExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.schema.GraphQLSchema;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

@Slf4j
public class GraphQLOrchestrator {

  public static final String DATA_LOADER_REGISTRY_CONTEXT_KEY = DataLoaderRegistry.class.getName() + ".context.key";

  private final RuntimeGraph runtimeGraph;
  private final List<Instrumentation> instrumentations;
  private final ExecutionIdProvider executionIdProvider;
  private final ExecutionStrategy queryExecutionStrategy;
  private final ExecutionStrategy mutationExecutionStrategy;

  private GraphQLOrchestrator(final RuntimeGraph runtimeGraph, final List<Instrumentation> instrumentations,
      final ExecutionIdProvider executionIdProvider, final ExecutionStrategy queryExecutionStrategy,
      final ExecutionStrategy mutationExecutionStrategy) {
    this.runtimeGraph = runtimeGraph;
    this.instrumentations = instrumentations;
    this.executionIdProvider = executionIdProvider;
    this.queryExecutionStrategy = queryExecutionStrategy;
    this.mutationExecutionStrategy = mutationExecutionStrategy;
  }

  public static GraphQLOrchestrator.Builder newOrchestrator() {
    return new Builder();
  }

  @SuppressWarnings("unchecked")
  private DataLoaderRegistry buildNewDataLoaderRegistry() {
    final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();

    // Important to make sure that the same instance of dataloader, (not batchloader)
    // is used for batching queries belonging to same provider. Also very very important
    // to create a new DataLoader per request. Else it will use the cache which is shared
    // across request.
    final Map<BatchLoader, DataLoader> temporaryMap = this.runtimeGraph.getBatchLoaderMap().values().stream().distinct()
        .collect(Collectors.toMap(Function.identity(), DataLoader::new));

    this.runtimeGraph.getBatchLoaderMap()
        .forEach((key, batchLoader) ->
            dataLoaderRegistry.register(key, temporaryMap.getOrDefault(batchLoader, new DataLoader(batchLoader))));
    return dataLoaderRegistry;
  }

  public CompletableFuture<ExecutionResult> execute(ExecutionInput executionInput) {

    final GraphQL.Builder graphqlBuilder = GraphQL.newGraphQL(runtimeGraph.getExecutableSchema())
        .instrumentation(new ChainedInstrumentation(instrumentations))
        .executionIdProvider(executionIdProvider)
        .queryExecutionStrategy(queryExecutionStrategy);

    if (Objects.nonNull(mutationExecutionStrategy)) {
      graphqlBuilder.mutationExecutionStrategy(mutationExecutionStrategy);
    }

    final GraphQL graphQL = graphqlBuilder.build();

    final ExecutionInput newExecutionInput = executionInput
        .transform(builder -> builder.dataLoaderRegistry(buildNewDataLoaderRegistry()));

    if (newExecutionInput.getContext() instanceof GraphQLContext) {
      ((GraphQLContext) executionInput.getContext())
          .put(DATA_LOADER_REGISTRY_CONTEXT_KEY, newExecutionInput.getDataLoaderRegistry());
    }
    return graphQL.executeAsync(newExecutionInput);
  }

  public GraphQLSchema getSchema() {
    return runtimeGraph.getExecutableSchema();
  }

  public CompletableFuture<ExecutionResult> execute(UnaryOperator<ExecutionInput.Builder> unaryOperator) {
    return execute(unaryOperator.apply(ExecutionInput.newExecutionInput()));
  }

  public CompletableFuture<ExecutionResult> execute(ExecutionInput.Builder executionInputBuilder) {
    return execute(executionInputBuilder.build());
  }

  public static class Builder {

    private RuntimeGraph runtimeGraph = null;
    private ExecutionIdProvider executionIdProvider = ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER;
    private ExecutionStrategy queryExecutionStrategy = new AsyncExecutionStrategy();
    private ExecutionStrategy mutationExecutionStrategy = null;
    private List<Instrumentation> instrumentations = new LinkedList<>(
        Arrays.asList(new DataLoaderDispatcherInstrumentation()));

    private Builder() {
    }

    public Builder runtimeGraph(final RuntimeGraph runtimeGraph) {
      this.runtimeGraph = requireNonNull(runtimeGraph);
      return this;
    }

    public Builder instrumentation(final Instrumentation instrumentation) {
      this.instrumentations.add(requireNonNull(instrumentation));
      return this;
    }

    public Builder instrumentation(int index, final Instrumentation instrumentation) {
      this.instrumentations.add(index, requireNonNull(instrumentation));
      return this;
    }

    public Builder instrumentations(final List<Instrumentation> instrumentations) {
      this.instrumentations.addAll(requireNonNull(instrumentations));
      return this;
    }

    public Builder executionIdProvider(final ExecutionIdProvider executionIdProvider) {
      this.executionIdProvider = requireNonNull(executionIdProvider);
      return this;
    }

    public Builder queryExecutionStrategy(final ExecutionStrategy queryExecutionStrategy) {
      this.queryExecutionStrategy = requireNonNull(queryExecutionStrategy);
      return this;
    }

    public Builder mutationExecutionStrategy(final ExecutionStrategy mutationExecutionStrategy) {
      this.mutationExecutionStrategy = requireNonNull(mutationExecutionStrategy);
      return this;
    }

    public GraphQLOrchestrator build() {
      return new GraphQLOrchestrator(runtimeGraph, instrumentations, executionIdProvider, queryExecutionStrategy,
          mutationExecutionStrategy);
    }
  }
}
