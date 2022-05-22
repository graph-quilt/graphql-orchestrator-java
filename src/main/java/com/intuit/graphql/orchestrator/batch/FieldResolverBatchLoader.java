package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FQN_FIELD_SEPARATOR;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.FQN_KEYWORD_QUERY;
import static com.intuit.graphql.orchestrator.resolverdirective.FieldResolverDirectiveUtil.createFieldResolverOperationName;
import static graphql.language.AstPrinter.printAstCompact;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.datafetcher.ServiceDataFetcher;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverBatchSelectionSetSupplier;
import com.intuit.graphql.orchestrator.fieldresolver.FieldResolverGraphQLError;
import com.intuit.graphql.orchestrator.fieldresolver.QueryOperationFactory;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext;
import com.intuit.graphql.orchestrator.xtext.FieldContext;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.language.AstTransformer;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.BatchLoader;

public class FieldResolverBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private final QueryOperationModifier queryOperationModifier = new QueryOperationModifier();

  private final QueryResponseModifier queryResponseModifier = new DefaultQueryResponseModifier();

  private final String[] resolverSelectedFields;

  private final FieldResolverContext fieldResolverContext;

  private final BatchResultTransformer batchResultTransformer;

  private final QueryOperationFactory queryOperationFactory = new QueryOperationFactory();

  private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

  @Builder
  public FieldResolverBatchLoader(FieldResolverContext fieldResolverContext) {
    Objects.requireNonNull(fieldResolverContext, "fieldResolverContext is required");
    Objects.requireNonNull(fieldResolverContext.getResolverDirectiveDefinition(),
        "resolverDirectiveDefinition is required");

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
            new FieldResolverBatchSelectionSetSupplier(resolverSelectedFields, dataFetchingEnvironments, fieldResolverContext);
    SelectionSet selectionSet = fieldResolverBatchSelectionSetSupplier.get();
    OperationDefinition downstreamQueryOpDef = queryOperationFactory.create(downstreamQueryOpName, selectionSet);

    ServiceMetadata serviceMetadata = getServiceMetadata(dataFetchingEnvironments.get(0));
    if (serviceMetadata.hasFieldResolverDirective()) {
      downstreamQueryOpDef = removeExternalFields(downstreamQueryOpDef, dataFetchingEnvironments.get(0), serviceMetadata);
    }

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

  private OperationDefinition removeExternalFields(OperationDefinition operationDefinition,
      DataFetchingEnvironment dataFetchingEnvironment, ServiceMetadata serviceMetadata) {
    GraphQLSchema graphQLSchema = dataFetchingEnvironment.getGraphQLSchema();
    GraphQLObjectType rootType = graphQLSchema.getQueryType();
    Map<String, FragmentDefinition> fragmentsByName = dataFetchingEnvironment.getFragmentsByName();
    return (OperationDefinition) AST_TRANSFORMER.transform(operationDefinition,
        new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) unwrapAll(rootType), serviceMetadata, fragmentsByName));
  }

  private ServiceMetadata getServiceMetadata(DataFetchingEnvironment dataFetchingEnvironment) {
    GraphQLSchema graphQLSchema = dataFetchingEnvironment.getGraphQLSchema();
    GraphQLCodeRegistry graphQLCodeRegistry = graphQLSchema.getCodeRegistry();

    int start = 0;
    if (FQN_KEYWORD_QUERY.equals(resolverSelectedFields[0])) {
      start = 1;
    }

    GraphQLFieldsContainer currentParentType = graphQLSchema.getQueryType();
    for (int i = start; i < resolverSelectedFields.length; i++) {
      String fieldName = resolverSelectedFields[i];
      GraphQLFieldDefinition graphQLFieldDefinition = currentParentType.getFieldDefinition(fieldName);
      FieldCoordinates fieldCoordinates = FieldCoordinates.coordinates(currentParentType.getName(), fieldName);
      DataFetcher<?> dataFetcher = graphQLCodeRegistry.getDataFetcher(fieldCoordinates, graphQLFieldDefinition);
      if (Objects.nonNull(dataFetcher) && dataFetcher instanceof ServiceDataFetcher) {
        ServiceDataFetcher serviceDataFetcher = (ServiceDataFetcher) dataFetcher;
        return serviceDataFetcher.getServiceMetadata();
      }
      if (!(graphQLFieldDefinition.getType() instanceof GraphQLFieldsContainer)) {
        break;
      }
      currentParentType = (GraphQLFieldsContainer) graphQLFieldDefinition.getType();
    }

    FieldContext fieldContext = this.fieldResolverContext.getTargetFieldContext();
    FieldCoordinates fieldCoordinates = fieldContext.getFieldCoordinates();
    throw FieldResolverGraphQLError.builder()
        .errorMessage("Service DataFetcher not found.")
        .fieldName(fieldCoordinates.getFieldName())
        .parentTypeName(fieldCoordinates.getTypeName())
        .resolverDirectiveDefinition(this.fieldResolverContext.getResolverDirectiveDefinition())
        .serviceNameSpace(fieldResolverContext.getServiceNamespace())
        .build();
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