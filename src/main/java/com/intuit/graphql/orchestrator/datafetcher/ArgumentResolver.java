package com.intuit.graphql.orchestrator.datafetcher;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.dataloader.DataLoaderRegistry;

/**
 * This class breaks up the ResolverArgumentDataFetcher class and it contains the logic to execute resolver queries to
 * get argument data.
 */
public class ArgumentResolver {

  private static final Function<GraphQLSchema, GraphQL> DEFAULT_GRAPHQL_BUILDER = schema -> GraphQL.newGraphQL(schema)
      .queryExecutionStrategy(new AsyncExecutionStrategy())
      .build();

  private Function<GraphQLSchema, GraphQL> graphQLBuilder;

  private ArgumentResolver(final Builder builder) {
    graphQLBuilder = builder.graphQLBuilder;
  }

  /**
   * Returns an ExecutionResult for each resolver argument query.
   * <p>
   * A new GraphQL is created and the queries are executed against the new GraphQL instance, using the
   * DataLoaderRegistry of the previous GraphQL instance (passed along through the GraphQLContext with key {@link
   * com.intuit.graphql.orchestrator.GraphQLOrchestrator#DATA_LOADER_REGISTRY_CONTEXT_KEY}).
   *
   * <p>
   * This method of "recursively" calling a new GraphQL instance within a GraphQL instance is a DRY principle
   * opportunity used to drastically reduce coding effort required to solve for problems that arise with the resolver
   * argument feature. Since query execution is already achieved by {@link GraphQL#execute(String)}, it does not make
   * sense to re-code query execution for resolver arguments. Since the GraphQL is a query executor and has all the
   * necessary information in order to execute queries, we can rely on GraphQL to perform query execution for our
   * purposes.
   *
   * <p>
   * The following problems are solved by executing GraphQL recursively:
   * <ul>
   *  <li>Identifying the destination service(s) to resolve the argument for the queries.</li>
   *  <li>Query traversal to resolve fields in query.</li>
   *  <li>Maintaining and populating a cache for values that already have been computed.</li>
   *  <li>Handling errors that arise due to execution failure.</li>
   *  <li>Optimal parallelization of service calls (already handled by the DataLoaderRegistry)</li>
   * </ul>
   *
   * @param env             the DataFetchingEnvironment for the field that requires resolver arguments
   * @param resolverQueries the queries used to call downstream services
   * @return A map of resolved arguments as CompletableFutures, mapped to the ResolverArgumentDirective.
   */
  public Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> resolveArguments(
      DataFetchingEnvironment env, Map<ResolverArgumentDirective, OperationDefinition> resolverQueries) {
    GraphQLContext graphQLContext = env.getContext();
    final DataLoaderRegistry originalDataLoaderRegistry = graphQLContext.get(DATA_LOADER_REGISTRY_CONTEXT_KEY);

    GraphQL graphQL = graphQLBuilder.apply(env.getGraphQLSchema());

    Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> resolverServiceFuturesByArgument = new HashMap<>();

    for (final Entry<ResolverArgumentDirective, OperationDefinition> entry : resolverQueries.entrySet()) {
      OperationDefinition query = entry.getValue();

      Document queryDocument = Document.newDocument().definition(query).build();

      ExecutionInput executionInput = ExecutionInput.newExecutionInput()
          .dataLoaderRegistry(originalDataLoaderRegistry)
          .context(graphQLContext)
          .query(AstPrinter.printAstCompact(queryDocument))
          .root(queryDocument)
          .build();

      resolverServiceFuturesByArgument.put(entry.getKey(), graphQL.executeAsync(executionInput));
    }

    return resolverServiceFuturesByArgument;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private Function<GraphQLSchema, GraphQL> graphQLBuilder = DEFAULT_GRAPHQL_BUILDER;

    private Builder() {
    }

    public Builder graphQLBuilder(final Function<GraphQLSchema, GraphQL> val) {
      graphQLBuilder = val;
      return this;
    }

    public ArgumentResolver build() {
      return new ArgumentResolver(this);
    }
  }
}
