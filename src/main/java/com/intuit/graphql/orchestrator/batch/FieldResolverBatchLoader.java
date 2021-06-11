package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;
import static graphql.language.AstPrinter.printAstCompact;

import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveQueryBuilder;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.transform.FieldWithResolverMetadata;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.DataFetcherResult;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.BatchLoader;

public class FieldResolverBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private static final Function<GraphQLSchema, GraphQL> DEFAULT_GRAPHQL_BUILDER = schema -> GraphQL
      .newGraphQL(schema)
      .queryExecutionStrategy(new AsyncExecutionStrategy())
      .build();

  private final ResolverDirectiveQueryBuilder resolverDirectiveQueryBuilder = new ResolverDirectiveQueryBuilder();

  private final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();

  private final String[] resolverSelectedFields;

  private final FieldWithResolverMetadata fieldWithResolverMetadata;

  private final ResolverDirectiveDefinition resolverDirectiveDefinition;

  private final BatchResultTransformer batchResultTransformer;

  @Builder
  public FieldResolverBatchLoader(FieldWithResolverMetadata fieldWithResolverMetadata) {
    Objects.requireNonNull(fieldWithResolverMetadata, "fieldWithResolverMetadata is required");
    Objects.requireNonNull(fieldWithResolverMetadata.getResolverDirectiveDefinition(),
        "resolverDirectiveDefinition is required");

    this.fieldWithResolverMetadata = fieldWithResolverMetadata;
    this.resolverDirectiveDefinition = fieldWithResolverMetadata.getResolverDirectiveDefinition();
    this.resolverSelectedFields = StringUtils.split(resolverDirectiveDefinition.getField(), '.');
    this.batchResultTransformer = new FieldResolverBatchResultTransformer(resolverSelectedFields, fieldWithResolverMetadata);
  }

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(final List<DataFetchingEnvironment> dataFetchingEnvironments) {

    OperationDefinition resolverQueryOpDef = resolverDirectiveQueryBuilder.buildFieldResolverQuery(
        resolverSelectedFields,
        resolverDirectiveDefinition,
        fieldWithResolverMetadata,
        dataFetchingEnvironments
    );

    if (this.fieldWithResolverMetadata.isRequiresTypeNameInjection()) {
      resolverQueryOpDef = queryOperationModifier.modifyQuery(
          dataFetchingEnvironments.get(0).getGraphQLSchema(),
          resolverQueryOpDef,
          // each DFE have identical fragmentsByName since this is a batch call for same field
          // Arguments on the field with @resolver now allowed, set variables empty
          dataFetchingEnvironments.get(0).getFragmentsByName(), Collections.emptyMap());
    }

    List<Definition<FragmentDefinition>> resolverQueryFragmentDefinitions =
        createResolverQueryFragmentDefinitions(dataFetchingEnvironments.get(0));

    return execute(dataFetchingEnvironments.get(0), resolverQueryOpDef, resolverQueryFragmentDefinitions)
        .thenApply(ExecutionResult::toSpecification)
        .thenApply(queryResponseModifier::modify)
        .thenApply(result -> batchResultTransformer.toBatchResult(result, dataFetchingEnvironments));
  }

  private List<Definition<FragmentDefinition>> createResolverQueryFragmentDefinitions(DataFetchingEnvironment dataFetchingEnvironment) {
    return dataFetchingEnvironment.getField().getSelectionSet().getSelections()
        .stream()
        .filter(selection -> selection instanceof FragmentSpread)
        .map(spread -> (FragmentSpread) spread)
        .map(spread -> dataFetchingEnvironment.getFragmentsByName().get(spread.getName()))
        .collect(Collectors.toList());
  }

  private CompletableFuture<ExecutionResult> execute(DataFetchingEnvironment dataFetchingEnvironment,
      OperationDefinition resolverQueryOpDef,
      List<Definition<FragmentDefinition>> resolverQueryFragmentDefs
  ) {
    GraphQLContext context = dataFetchingEnvironment.getContext();

    Document resolverQueryDoc = Document.newDocument()
        .definitions(resolverQueryFragmentDefs.stream().map(GraphQLObjects::<Definition<FragmentDefinition>>cast)
            .collect(Collectors.toList()))
        .definition(resolverQueryOpDef)
        .build();

    ExecutionInput resolverQueryExecutionInput = ExecutionInput.newExecutionInput()
        .dataLoaderRegistry(context.get(DATA_LOADER_REGISTRY_CONTEXT_KEY))
        .context(context)
        .root(resolverQueryDoc)
        .query(printAstCompact(resolverQueryDoc))
        .operationName(resolverQueryOpDef.getName())
        .build();

    GraphQL graphQL = DEFAULT_GRAPHQL_BUILDER.apply(dataFetchingEnvironment.getGraphQLSchema());

    return graphQL.executeAsync(resolverQueryExecutionInput);
  }

}