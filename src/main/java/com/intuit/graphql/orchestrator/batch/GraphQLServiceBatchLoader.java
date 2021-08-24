package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.authorization.AuthorizationContext;
import com.intuit.graphql.orchestrator.authorization.DefaultFieldAuthorization;
import com.intuit.graphql.orchestrator.authorization.FieldAuthorization;
import com.intuit.graphql.orchestrator.batch.MergedFieldModifier.MergedFieldModifierResult;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.VisibleForTesting;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.*;
import graphql.language.OperationDefinition.Operation;
import graphql.schema.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dataloader.BatchLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.schema.transform.DomainTypesTransformer.DELIMITER;
import static graphql.language.AstPrinter.printAstCompact;
import static graphql.language.OperationDefinition.Operation.QUERY;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static java.util.Objects.requireNonNull;

public class GraphQLServiceBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private final QueryExecutor queryExecutor;
  private final QueryResponseModifier queryResponseModifier;
  private final BatchResultTransformer batchResultTransformer;
  private final QueryOperationModifier queryOperationModifier;
  private final ServiceMetadata serviceMetadata;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks;

  private static final AstTransformer AST_TRANSFORMER = new AstTransformer();

  @VisibleForTesting
  VariableDefinitionFilter variableDefinitionFilter = new VariableDefinitionFilter();

  private GraphQLServiceBatchLoader(Builder builder) {
    this.queryExecutor = builder.queryExecutor;
    this.queryResponseModifier = builder.queryResponseModifier;
    this.batchResultTransformer = builder.batchResultTransformer;
    this.queryOperationModifier = builder.queryOperationModifier;
    this.serviceMetadata = builder.serviceMetadata;
    this.hooks = builder.hooks;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(final List<DataFetchingEnvironment> keys) {
    GraphQLContext graphQLContext = getContext(keys);

    AuthorizationContext authorizationContext = graphQLContext.get(AuthorizationContext.class);

    if (Objects.nonNull(authorizationContext)) {
      CompletableFuture<Object> futureClaimData = authorizationContext.getFutureClaimData();
      return futureClaimData.thenCompose(claimData -> {
        Pair<String, Object> claimDataMap = ImmutablePair.of(authorizationContext.getClaimDataName(), claimData);
        return load(keys, claimDataMap, authorizationContext, graphQLContext, true);
      });
    } else {
      return load(keys, graphQLContext);
    }
  }

  private CompletionStage<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> keys, GraphQLContext graphQLContext) {
    AuthorizationContext<Object> authorizationContext = new AuthorizationContext<>(new DefaultFieldAuthorization());
    return this.load(keys, ImmutablePair.nullPair(), authorizationContext, graphQLContext, false);
  }

  private CompletableFuture<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> keys,
                                                                  Pair claimData,
                                                                  AuthorizationContext authorizationContext,
                                                                  GraphQLContext context,
                                                                  boolean fieldAuthorizationEnabled) {
    hooks.onBatchLoadStart(context, keys);

    Optional<OperationDefinition> operation = getFirstOperation(keys);
    Operation operationType = operation.map(OperationDefinition::getOperation).orElse(QUERY);
    String operationName = operation.map(OperationDefinition::getName).orElse(operationType.toString());

    GraphQLSchema graphQLSchema = getSchema(keys);

    List<Directive> operationDirectives = operation.map(OperationDefinition::getDirectives)
            .orElse(Collections.emptyList());
    GraphQLObjectType operationObjectType =
            operationType == QUERY ? graphQLSchema.getQueryType() : graphQLSchema.getMutationType();

    SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();

    Map<String, FragmentDefinition> mergedFragmentDefinitions = new HashMap<>();

    for (final DataFetchingEnvironment key : keys) {
      MergedFieldModifierResult result = new MergedFieldModifier(key).getFilteredRootField();
      MergedField filteredRootField = result.getMergedField();
      if (filteredRootField != null) {
        filteredRootField.getFields().stream()
                .map(field -> serviceMetadata.hasFieldResolverDirective() || fieldAuthorizationEnabled
                        ? removeFieldsWithExternalTypes(field, getRootFieldDefinition(key.getExecutionStepInfo()).getType(), claimData, authorizationContext, context)
                        : field
                )
                .forEach(selectionSetBuilder::selection);
      }

      for (final FragmentDefinition fragmentDefinition : result.getFragmentDefinitions().values()) {
        if (mergedFragmentDefinitions.containsKey(fragmentDefinition.getName())) {
          FragmentDefinition old = mergedFragmentDefinitions.get(fragmentDefinition.getName());

          List<Selection> newSelections = fragmentDefinition.getSelectionSet().getSelections();
          List<Selection> oldSelections = old.getSelectionSet().getSelections();

          oldSelections.addAll(newSelections);

          SelectionSet newSelectionSet = old.getSelectionSet().transform(builder -> builder.selections(oldSelections));

          mergedFragmentDefinitions.put(old.getName(), old.transform(builder -> builder.selectionSet(newSelectionSet)));
        } else {
          mergedFragmentDefinitions.put(fragmentDefinition.getName(), fragmentDefinition);
        }
      }

      Map<String, FragmentDefinition> svcFragmentDefinitions = filterFragmentDefinitionByService(
              key.getFragmentsByName());
      if (serviceMetadata.hasFieldResolverDirective() || fieldAuthorizationEnabled) {
        Map<String, FragmentDefinition> finalServiceFragmentDefinitions = new HashMap<>();
        svcFragmentDefinitions.forEach((fragmentName, fragmentDefinition) -> {
          String typeConditionName = fragmentDefinition.getTypeCondition().getName();
          GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) graphQLSchema.getType(typeConditionName);
          FragmentDefinition transformedFragment = removeFieldsWithExternalTypes(fragmentDefinition, parentType, claimData, authorizationContext, context);
          finalServiceFragmentDefinitions.put(fragmentName, removeDomainTypeFromFragment(transformedFragment));  // TODO handle, potentially could be empty
        });
        mergedFragmentDefinitions.putAll(finalServiceFragmentDefinitions);
      } else {
        mergedFragmentDefinitions.putAll(svcFragmentDefinitions);
      }
    }

    final SelectionSet filteredSelection = selectionSetBuilder.build(); // TODO handle, potentially could be empty

    Map<String, Object> mergedVariables = new HashMap<>();
    keys.stream()
            .flatMap(dataFetchingEnvironment -> dataFetchingEnvironment.getVariables().entrySet().stream())
            .distinct()
            .forEach(entry -> mergedVariables.put(entry.getKey(), entry.getValue()));

    List<VariableDefinition> variableDefinitions = keys.stream()
            .map(DataFetchingEnvironment::getOperationDefinition)
            .filter(Objects::nonNull)
            .flatMap(operationDefinition -> operationDefinition.getVariableDefinitions().stream())
            .distinct()
            .collect(Collectors.toList());

    List<Definition> fragmentsAsDefinitions = mergedFragmentDefinitions.values().stream()
            .map(GraphQLObjects::<Definition>cast).collect(Collectors.toList());

    OperationDefinition query = OperationDefinition.newOperationDefinition()
            .name(operationName)
            .variableDefinitions(variableDefinitions)
            .selectionSet(filteredSelection)
            .operation(operationType)
            .directives(operationDirectives)
            .build();

    if (!variableDefinitions.isEmpty()) {
      final Set<String> foundVariableReferences = variableDefinitionFilter.getVariableReferencesFromNode(
              graphQLSchema,
              operationObjectType,
              mergedFragmentDefinitions,
              mergedVariables,
              query
      );

      List<VariableDefinition> filteredVariableDefinitions = variableDefinitions.stream()
              .filter(variableDefinition -> foundVariableReferences.contains(variableDefinition.getName()))
              .collect(Collectors.toList());

      query = query.transform(builder -> builder.variableDefinitions(filteredVariableDefinitions));
    }

    if (serviceMetadata.requiresTypenameInjection()) {
      query = queryOperationModifier.modifyQuery(graphQLSchema, query, mergedFragmentDefinitions, mergedVariables);
    }

    return execute(context, query, mergedVariables, fragmentsAsDefinitions)
            .thenApply(queryResponseModifier::modify)
            .thenApply(result -> batchResultTransformer.toBatchResult(result, keys))
            .thenApply(batchResult -> {
              hooks.onBatchLoadEnd(context, batchResult);
              return batchResult;
            });
  }

  private GraphQLFieldDefinition getRootFieldDefinition(ExecutionStepInfo executionStepInfo) {
    ExecutionStepInfo currExecutionStepInfo = executionStepInfo;
    while (currExecutionStepInfo.getParent().hasParent()) {
      currExecutionStepInfo = currExecutionStepInfo.getParent();
    }
    return currExecutionStepInfo.getFieldDefinition();
  }

  private Map<String, FragmentDefinition> filterFragmentDefinitionByService(
      Map<String, FragmentDefinition> fragmentsByName) {

    Map<String, FragmentDefinition> map = new HashMap<>();

    fragmentsByName.keySet().forEach(key -> {
      FragmentDefinition fragmentDefinition = fragmentsByName.get(key);
      if (serviceMetadata.hasType(fragmentDefinition.getTypeCondition().getName())) {
        map.put(key, fragmentDefinition);
      }
    });
    return map;
  }

  private CompletableFuture<Map<String, Object>> execute(GraphQLContext context, OperationDefinition queryOp,
      final Map<String, Object> variables, List<Definition> fragmentDefinitions) {
    Document document = Document.newDocument()
        .definitions(fragmentDefinitions)
        .definition(queryOp)
        .build();

    ExecutionInput i = ExecutionInput.newExecutionInput()
        .context(context)
        .root(document)
        .query(printAstCompact(document))
        .operationName(queryOp.getName())
        .variables(variables)
        .build();

    hooks.onExecutionInput(context, i);

    if (queryOp.getSelectionSet().getSelections().isEmpty()) {
      return CompletableFuture.completedFuture(new HashMap<>());
    }

    return this.queryExecutor.query(i, context)
        .thenApply(result -> {
          hooks.onQueryResult(context, result);
          return result;
        });
  }

  /**
   * remove fields with external type from a fragment definition.
   *
   * @param origFragmentDefinition original fragment definition
   * @param typeCondition type condition of the original fragment definition.  This will be used as
   *                      the root type for {@link NoExternalReferenceSelectionSetModifier}
   * @return a modified fragment definition
   */
  private FragmentDefinition removeFieldsWithExternalTypes(final FragmentDefinition origFragmentDefinition,
      GraphQLType typeCondition, Pair claimData, AuthorizationContext authorizationContext, GraphQLContext graphQLContext) {
    // call serviceMetadata.hasFieldResolverDirective() before calling this method
    return (FragmentDefinition) AST_TRANSFORMER.transform(origFragmentDefinition,
        new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) unwrapAll(typeCondition), claimData, authorizationContext, graphQLContext));

  }

  /**
   * remove domain prefix from fragment definition.
   *
   * @param origFragmentDefinition original fragment definition
   *
   * @return a modified fragment definition
   */
  private FragmentDefinition removeDomainTypeFromFragment(final FragmentDefinition origFragmentDefinition) {
    TypeName typeCondition = origFragmentDefinition.getTypeCondition();
    String typeConditionName = typeCondition.getName();
    final String domainPrefix = serviceMetadata.getServiceProvider().getNameSpace() + DELIMITER;
    if (StringUtils.startsWith(typeConditionName, domainPrefix)) {
      return origFragmentDefinition.transform(builder -> builder.typeCondition(
          typeCondition.transform(bdr -> bdr.name(StringUtils.removeStart(typeConditionName, domainPrefix)))
      ));
    }
    return origFragmentDefinition;
  }

  /**
   * remove fields with external type from selections of a given field.
   *
   * @param origField field to be processed.
   * @param fieldType the type of origField.  This will be used as the root type for {@link
   * NoExternalReferenceSelectionSetModifier}
   * @return a modified field
   */
  private Field removeFieldsWithExternalTypes(Field origField, GraphQLOutputType fieldType, Pair claimData,
                                              AuthorizationContext authorizationContext, GraphQLContext graphQLContext) {
    // call serviceMetadata.hasFieldResolverDirective() before calling this method
    return (Field) AST_TRANSFORMER.transform(origField,
        new NoExternalReferenceSelectionSetModifier((GraphQLFieldsContainer) unwrapAll(fieldType), claimData, authorizationContext, graphQLContext));
  }

  private GraphQLSchema getSchema(List<DataFetchingEnvironment> environments) {
    return environments.stream()
        .map(DataFetchingEnvironment::getGraphQLSchema)
        .findFirst()
        .orElse(null);
  }

  private GraphQLContext getContext(List<DataFetchingEnvironment> environments) {
    return environments.stream()
        .map(DataFetchingEnvironment::<GraphQLContext>getContext)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private Optional<OperationDefinition> getFirstOperation(List<DataFetchingEnvironment> environments) {
    return environments.stream()
        .map(DataFetchingEnvironment::getOperationDefinition)
        .filter(Objects::nonNull)
        .findFirst();
  }

  public static GraphQLServiceBatchLoader.Builder newQueryExecutorBatchLoader() {
    return new Builder();
  }

  public static class Builder {

    public static final QueryResponseModifier defaultQueryResponseModifier = new DefaultQueryResponseModifier();
    public static final BatchResultTransformer defaultBatchResultTransformer = new SubtreeBatchResultTransformer();
    public static final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> defaultHooks = BatchLoaderExecutionHooks.DEFAULT_HOOKS;

    private Builder() {

    }

    private QueryExecutor queryExecutor;
    private ServiceMetadata serviceMetadata;
    private QueryResponseModifier queryResponseModifier = defaultQueryResponseModifier;
    private BatchResultTransformer batchResultTransformer = defaultBatchResultTransformer;
    private QueryOperationModifier queryOperationModifier = new QueryOperationModifier();
    private BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks = defaultHooks;

    public Builder queryExecutor(final QueryExecutor queryExecutor) {
      this.queryExecutor = requireNonNull(queryExecutor);
      return this;
    }

    public Builder serviceMetadata(final ServiceMetadata serviceMetadata) {
      this.serviceMetadata = requireNonNull(serviceMetadata);
      return this;
    }

    public Builder queryResponseModifier(final QueryResponseModifier queryResponseModifier) {
      this.queryResponseModifier = requireNonNull(queryResponseModifier);
      return this;
    }

    public Builder batchResultTransformer(final BatchResultTransformer batchResultTransformer) {
      this.batchResultTransformer = requireNonNull(batchResultTransformer);
      return this;
    }

    public Builder queryOperationModifier(final QueryOperationModifier queryOperationModifier) {
      this.queryOperationModifier = requireNonNull(queryOperationModifier);
      return this;
    }

    public Builder batchLoaderExecutionHooks(
        BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks) {
      this.hooks = requireNonNull(hooks);
      return this;
    }

    public GraphQLServiceBatchLoader build() {
      return new GraphQLServiceBatchLoader(this);
    }
  }
}