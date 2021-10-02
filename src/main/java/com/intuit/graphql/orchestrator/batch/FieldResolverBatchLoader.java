package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createFieldResolverOperationName;
import static graphql.language.AstPrinter.printAstCompact;

import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverBatchSelectionSetSupplier;
import com.intuit.graphql.orchestrator.fieldresolver.QueryOperationFactory;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
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
import graphql.language.SelectionSet;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.BatchLoader;

public class FieldResolverBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private static final Function<GraphQLSchema, GraphQL> DEFAULT_GRAPHQL_BUILDER = schema -> GraphQL
      .newGraphQL(schema)
      .queryExecutionStrategy(new AsyncExecutionStrategy())
      .build();

  private final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();

  private final String[] resolverSelectedFields;

  private final FieldResolverContext fieldResolverContext;

  private final BatchResultTransformer batchResultTransformer;

  private final QueryOperationFactory queryOperationFactory = new QueryOperationFactory();

  @Builder
  public FieldResolverBatchLoader(FieldResolverContext fieldResolverContext) {
    Objects.requireNonNull(fieldResolverContext, "fieldResolverContext is required");
    Objects.requireNonNull(fieldResolverContext.getResolverDirectiveDefinition(),
        "resolverDirectiveDefinition is required");

    this.fieldResolverContext = fieldResolverContext;
    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();

    this.resolverSelectedFields = StringUtils.split(resolverDirectiveDefinition.getField(), '.');
    this.batchResultTransformer = new FieldResolverBatchResultTransformer(resolverSelectedFields, fieldResolverContext);
  }

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(final List<DataFetchingEnvironment> dataFetchingEnvironments) {

    String originalOperationName = dataFetchingEnvironments.get(0).getOperationDefinition().getName();
    String operationName = createFieldResolverOperationName(originalOperationName);

    FieldResolverBatchSelectionSetSupplier fieldResolverBatchSelectionSetSupplier =
            new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments, fieldResolverContext);

    OperationDefinition resolverQueryOpDef = queryOperationFactory.create(operationName, fieldResolverBatchSelectionSetSupplier);

    if (this.fieldResolverContext.isRequiresTypeNameInjection()) {
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
    SelectionSet selectionSet = dataFetchingEnvironment.getField().getSelectionSet();
    if (selectionSet == null || CollectionUtils.isEmpty(selectionSet.getSelections())) {
      return Collections.emptyList();
    }
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