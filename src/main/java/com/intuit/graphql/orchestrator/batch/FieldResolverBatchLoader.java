package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverBatchSelectionSetSupplier;
import com.intuit.graphql.orchestrator.fieldresolver.QueryOperationFactory;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.BatchLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FQN_FIELD_SEPARATOR;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createFieldResolverOperationName;
import static graphql.language.AstPrinter.printAstCompact;

public class FieldResolverBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();

  private final String[] resolverSelectedFields;

  private final FieldResolverContext fieldResolverContext;

  private final BatchResultTransformer batchResultTransformer;

  private final ServiceMetadata serviceMetadata;

  private final QueryOperationFactory queryOperationFactory = new QueryOperationFactory();


  @Builder
  public FieldResolverBatchLoader(FieldResolverContext fieldResolverContext, ServiceMetadata serviceMetadata) {
    Objects.requireNonNull(fieldResolverContext, "fieldResolverContext is required");
    Objects.requireNonNull(fieldResolverContext.getResolverDirectiveDefinition(),
        "resolverDirectiveDefinition is required");
    Objects.requireNonNull(serviceMetadata, "serviceMetadata is required");
    Objects.requireNonNull(serviceMetadata.getServiceProvider(), "serviceProvider is required");

    this.serviceMetadata = serviceMetadata;
    this.fieldResolverContext = fieldResolverContext;
    ResolverDirectiveDefinition resolverDirectiveDefinition = fieldResolverContext.getResolverDirectiveDefinition();

    this.resolverSelectedFields = StringUtils.split(resolverDirectiveDefinition.getField(), FQN_FIELD_SEPARATOR);
    this.batchResultTransformer = new FieldResolverBatchResultTransformer(resolverSelectedFields, fieldResolverContext);
  }

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(final List<DataFetchingEnvironment> dataFetchingEnvironments) {

    String originalOperationName = dataFetchingEnvironments.get(0).getOperationDefinition().getName();
    String downstreamQueryOpName = createFieldResolverOperationName(originalOperationName);

    FieldResolverBatchSelectionSetSupplier fieldResolverBatchSelectionSetSupplier =
            new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields,
                dataFetchingEnvironments, fieldResolverContext, serviceMetadata);

    SelectionSet selectionSet = fieldResolverBatchSelectionSetSupplier.get();
    OperationDefinition downstreamQueryOpDef = queryOperationFactory.create(downstreamQueryOpName, selectionSet);

    if (this.fieldResolverContext.isRequiresTypeNameInjection()) {
      downstreamQueryOpDef = queryOperationModifier.modifyQuery(
          dataFetchingEnvironments.get(0).getGraphQLSchema(),
          downstreamQueryOpDef,
          // each DFE have identical fragmentsByName since this is a batch call for same field
          // Arguments on the field with @resolver now allowed, set variables empty
          dataFetchingEnvironments.get(0).getFragmentsByName(), Collections.emptyMap());
    }

    List<Definition<FragmentDefinition>> downstreamQueryFragmentDefinitions =
        createResolverQueryFragmentDefinitions(dataFetchingEnvironments.get(0));

    ServiceProvider serviceProvider = serviceMetadata.getServiceProvider();
    return execute(dataFetchingEnvironments.get(0), downstreamQueryOpDef, downstreamQueryFragmentDefinitions, serviceProvider)
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
        .filter(FragmentSpread.class::isInstance)
        .map(FragmentSpread.class::cast)
        .map(FragmentSpread::getName)
        .map(dataFetchingEnvironment.getFragmentsByName()::get)
        .collect(Collectors.toList());
  }

  private CompletableFuture<Map<String, Object>> execute(DataFetchingEnvironment dataFetchingEnvironment,
      OperationDefinition resolverQueryOpDef,
      List<Definition<FragmentDefinition>> resolverQueryFragmentDefs,
      ServiceProvider serviceProvider
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

    return serviceProvider.query(resolverQueryExecutionInput, dataFetchingEnvironment.getContext());
  }

}