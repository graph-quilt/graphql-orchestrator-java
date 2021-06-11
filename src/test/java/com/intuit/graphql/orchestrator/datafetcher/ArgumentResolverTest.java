package com.intuit.graphql.orchestrator.datafetcher;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;
import static com.intuit.graphql.orchestrator.TestHelper.query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoaderRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class ArgumentResolverTest {

  private ArgumentResolver argumentResolver;

  @Mock
  public GraphQL mockedGraphQL;

  @Before
  public void setUp() {
    initMocks(this);
    argumentResolver = ArgumentResolver.newBuilder()
        .graphQLBuilder(schema -> mockedGraphQL)
        .build();
  }

  @Test
  public void resolvesArguments() {
    ArgumentCaptor<ExecutionInput> executionInput = ArgumentCaptor.forClass(ExecutionInput.class);

    ExecutionResult aResult = ExecutionResultImpl.newExecutionResult()
        .data(Collections.emptyMap()).build();

    doReturn(CompletableFuture.completedFuture(aResult))
        .when(mockedGraphQL).executeAsync(executionInput.capture());

    OperationDefinition aQuery = query("{ a }");
    OperationDefinition bQuery = query("{ b }");

    Map<ResolverArgumentDirective, OperationDefinition> resolverQueries = new HashMap<>();

    resolverQueries.put(mock(ResolverArgumentDirective.class), aQuery);
    resolverQueries.put(mock(ResolverArgumentDirective.class), bQuery);

    GraphQLContext graphQLContext = GraphQLContext.newContext().of(DATA_LOADER_REGISTRY_CONTEXT_KEY, mock(
        DataLoaderRegistry.class)).build();

    DataFetchingEnvironment env = DataFetchingEnvironmentImpl
        .newDataFetchingEnvironment()
        .context(graphQLContext)
        .graphQLSchema(mock(GraphQLSchema.class))
        .build();
    final Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> results = argumentResolver
        .resolveArguments(env, resolverQueries);

    assertThat(results)
        .hasSize(2);
  }
}