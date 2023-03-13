package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.authorization.DefaultFieldAuthorization;
import com.intuit.graphql.orchestrator.authorization.DownstreamQueryRedactor;
import com.intuit.graphql.orchestrator.authorization.DownstreamQueryRedactorResult;
import com.intuit.graphql.orchestrator.authorization.FieldAuthorization;
import com.intuit.graphql.orchestrator.batch.MergedFieldModifier.MergedFieldModifierResult;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.GraphqlErrorException;
import graphql.VisibleForTesting;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.BatchLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.intuit.graphql.orchestrator.schema.transform.DomainTypesTransformer.DELIMITER;
import static com.intuit.graphql.orchestrator.utils.DirectivesUtil.USE_DEFER;
import static graphql.language.AstPrinter.printAstCompact;
import static graphql.language.OperationDefinition.Operation.QUERY;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static java.util.Objects.requireNonNull;

public class GraphQLServiceBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private final FieldAuthorization DEFAULT_FIELD_AUTHORIZATION = new DefaultFieldAuthorization();
  private final QueryExecutor queryExecutor;
  private final QueryResponseModifier queryResponseModifier;
  private final BatchResultTransformer batchResultTransformer;
  private final QueryOperationModifier queryOperationModifier;
  private final ServiceMetadata serviceMetadata;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks;
  private DownStreamQueryOptimizer queryOptimizer;

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

  @Override
  public CompletionStage<List<DataFetcherResult<Object>>> load(final List<DataFetchingEnvironment> keys) {

    GraphQLContext graphQLContext = getContext(keys);
    FieldAuthorization fieldAuthorization  = getDefaultOrCustomFieldAuthorization(graphQLContext);
    CompletableFuture<Object> futureAuthData = fieldAuthorization.getFutureAuthData();
    return futureAuthData.thenCompose(authData -> load(keys, graphQLContext, authData,
        fieldAuthorization));
  }

  private FieldAuthorization getDefaultOrCustomFieldAuthorization(GraphQLContext graphQLContext) {
    FieldAuthorization ctxFieldAuthorization = graphQLContext.get(FieldAuthorization.class);
    return Objects.isNull(ctxFieldAuthorization)
        ? DEFAULT_FIELD_AUTHORIZATION : ctxFieldAuthorization;
  }

  private CompletableFuture<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> keys,
      GraphQLContext context, Object authData, FieldAuthorization fieldAuthorization) {

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

    //Possibly add a complex object to hold these two
    MultiValuedMap<String, GraphqlErrorException> queryRedactErrorsByKey = new ArrayListValuedHashMap<>();
    Set<String> removedFragments = new HashSet<>();

    for (final DataFetchingEnvironment key : keys) {
      MergedFieldModifierResult result = new MergedFieldModifier(key).getFilteredRootField();
      MergedField filteredRootField = result.getMergedField();
      if (filteredRootField != null) {
        filteredRootField.getFields().stream()
          .map(field -> removeFieldsWithExternalTypes(field,
                operationObjectType, key, authData, fieldAuthorization, queryRedactErrorsByKey, removedFragments, context))
          .filter(Objects::nonNull) // denied access or has an empty selectionSet
          .forEach(selectionSetBuilder::selection);
      }

      for (final FragmentDefinition fragmentDefinition : result.getFragmentDefinitions().values()) {
        if (mergedFragmentDefinitions.containsKey(fragmentDefinition.getName())) {
          FragmentDefinition old = mergedFragmentDefinitions.get(fragmentDefinition.getName());

          List<Selection> newSelections = fragmentDefinition.getSelectionSet().getSelections();
          List<Selection> oldSelections = new ArrayList<>(old.getSelectionSet().getSelections());

          oldSelections.addAll(newSelections);

          SelectionSet newSelectionSet = old.getSelectionSet().transform(builder -> builder.selections(oldSelections));

          mergedFragmentDefinitions.put(old.getName(), old.transform(builder -> builder.selectionSet(newSelectionSet)));
        } else {
          mergedFragmentDefinitions.put(fragmentDefinition.getName(), fragmentDefinition);
        }
      }

      Map<String, FragmentDefinition> svcFragmentDefinitions = filterFragmentDefinitionByService(
          key.getFragmentsByName());
      if (serviceMetadata.hasFieldResolverDirective() || serviceMetadata.isFederationService() ||
          !(fieldAuthorization instanceof DefaultFieldAuthorization)) {
        Map<String, FragmentDefinition> finalServiceFragmentDefinitions = new HashMap<>();
        svcFragmentDefinitions.forEach((fragmentName, fragmentDefinition) -> {
          String typeConditionName = fragmentDefinition.getTypeCondition().getName();
          GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) graphQLSchema.getType(typeConditionName);
            FragmentDefinition transformedFragment = redactFragmentDefinition(fragmentDefinition, parentType,
              authData, fieldAuthorization, key, queryRedactErrorsByKey, operationObjectType);
          finalServiceFragmentDefinitions.put(fragmentName, removeDomainTypeFromFragment(transformedFragment));
        });
        mergedFragmentDefinitions.putAll(finalServiceFragmentDefinitions);
      } else {
        mergedFragmentDefinitions.putAll(svcFragmentDefinitions);
      }
    }

    SelectionSet filteredSelection = selectionSetBuilder.build();
    if (CollectionUtils.isEmpty(filteredSelection.getSelections())) {
      List<DataFetcherResult<Object>> batchResult = keys.stream()
        .map( key -> {
          String keyPath = key.getExecutionStepInfo().getPath().toString();
          Collection<GraphqlErrorException> graphqlErrorsColl = queryRedactErrorsByKey.get(keyPath);
          return DataFetcherResult.newResult()
          .errors(new ArrayList<>(graphqlErrorsColl))
          .build();
        })
        .collect(Collectors.toList());
      return CompletableFuture.completedFuture(batchResult);
    }
    queryOptimizer = new DownStreamQueryOptimizer(operationType);
    filteredSelection = queryOptimizer.getTransformedSelectionSet(filteredSelection);

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
        .filter(fragment -> !removedFragments.contains(fragment.getName()))
        .map(GraphQLObjects::<Definition>cast).collect(Collectors.toList());

    OperationDefinition query = OperationDefinition.newOperationDefinition()
        .name(operationName)
        .variableDefinitions(variableDefinitions)
        .selectionSet(filteredSelection)
        .operation(operationType)
        .directives(operationDirectives)
        .build();

    Map<String, Object> filteredVariables = mergedVariables;

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

      filteredVariables = filterVariables(filteredVariableDefinitions, mergedVariables);

      query = query.transform(builder -> builder.variableDefinitions(filteredVariableDefinitions));
    }

    if (serviceMetadata.requiresTypenameInjection()) {
      query = queryOperationModifier.modifyQuery(graphQLSchema, query, mergedFragmentDefinitions, filteredVariables);
    }


    return execute(context, query, filteredVariables, fragmentsAsDefinitions)
        .thenApply(queryResponseModifier::modify)
        .thenApply(result -> batchResultTransformer.toBatchResult(result, keys))
        .thenApply(batchResult -> addQueryRedactErrors(batchResult, queryRedactErrorsByKey, keys))
        .thenApply(batchResult -> {
          hooks.onBatchLoadEnd(context, batchResult);
          return batchResult;
        });
  }

  private List<DataFetcherResult<Object>> addQueryRedactErrors(
      List<DataFetcherResult<Object>> batchResult,
      MultiValuedMap<String, GraphqlErrorException> queryRedactErrorsByKey,
      List<DataFetchingEnvironment> keys) {
    if (!queryRedactErrorsByKey.isEmpty()) {
      for (int i = 0; i < keys.size(); i++) {
        final DataFetchingEnvironment key = keys.get(i);
        String keyPath = key.getExecutionStepInfo().getPath().toString();
        Collection<GraphqlErrorException> graphqlErrorsColl = queryRedactErrorsByKey.get(keyPath);
        if (CollectionUtils.isNotEmpty(graphqlErrorsColl)) {
          DataFetcherResult<Object> newDataFetcherResult = batchResult.get(i).transform(builder ->
            builder.errors(new ArrayList<>(graphqlErrorsColl))
        );
        batchResult.set(i, newDataFetcherResult);
        }
      }
    }
    return batchResult;
  }

  private Map<String, Object> filterVariables(List<VariableDefinition> filteredVariableDefinitions,
      Map<String, Object> mergedVariables) {
    if (MapUtils.isEmpty(mergedVariables)) {
      return mergedVariables;
    }

    Map<String, Object> output = new HashMap<>();
    filteredVariableDefinitions
        .forEach(variableDefinition -> {
          String variableDefinitionName = variableDefinition.getName();
          if (mergedVariables.containsKey(variableDefinitionName)) {
            output.put(variableDefinitionName, mergedVariables.get(variableDefinitionName));
          }
        });
    return output;
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

  private FragmentDefinition redactFragmentDefinition(final FragmentDefinition origFragmentDefinition,
      GraphQLType typeCondition, Object authData, FieldAuthorization fieldAuthorization,
      DataFetchingEnvironment dataFetchingEnvironment, MultiValuedMap<String, GraphqlErrorException> queryRedactErrorsByKey,
      GraphQLObjectType operationType) {

    DownstreamQueryRedactor fragmentDefinitionRedactor = DownstreamQueryRedactor.builder()
        .root(origFragmentDefinition)
        .rootType(unwrapAll(typeCondition))
        .rootParentType(operationType) // parent node of a fragment definition is the operation
        .authData(authData)
        .fieldAuthorization(fieldAuthorization)
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .serviceMetadata(serviceMetadata)
        .build();

    DownstreamQueryRedactorResult redactResult = fragmentDefinitionRedactor.redact();
    if (CollectionUtils.isNotEmpty(redactResult.getErrors())) {
      String keyPath = dataFetchingEnvironment.getExecutionStepInfo().getPath().toString();
      queryRedactErrorsByKey.putAll(keyPath, redactResult.getErrors());
    }
    return (FragmentDefinition) redactResult.getNode();
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

  private Field removeFieldsWithExternalTypes(Field origField, GraphQLObjectType operationObjectType,
      DataFetchingEnvironment dfe, Object authData, FieldAuthorization fieldAuthorization,
      final MultiValuedMap<String, GraphqlErrorException> queryRedactErrorsByKey, Set<String> removedFragments, GraphQLContext context) {
    
    if (serviceMetadata.shouldModifyDownStreamQuery() ||
        !(fieldAuthorization instanceof DefaultFieldAuthorization) ||
            context.getOrDefault(USE_DEFER, false)
    ) {
      GraphQLType origFieldType = getRootFieldDefinition(dfe.getExecutionStepInfo()).getType();
      DownstreamQueryRedactor downstreamQueryRedactor = DownstreamQueryRedactor.builder()
          .root(origField)
          .rootType(unwrapAll(origFieldType))
          .rootParentType(operationObjectType)
          .authData(authData)
          .fieldAuthorization(fieldAuthorization)
          .dataFetchingEnvironment(dfe)
          .serviceMetadata(serviceMetadata)
          .build();
      DownstreamQueryRedactorResult redactResult = downstreamQueryRedactor.redact();
      if (CollectionUtils.isNotEmpty(redactResult.getErrors())) {
        String keyPath = dfe.getExecutionStepInfo().getPath().toString();
        queryRedactErrorsByKey.putAll(keyPath, redactResult.getErrors());
      }
      if (CollectionUtils.isNotEmpty(redactResult.getFragmentSpreadsRemoved())) {
        removedFragments.addAll(redactResult.getFragmentSpreadsRemoved());
      }

      if (redactResult.isHasEmptySelectionSet()) {
        return null; // if null, not added to selection
      }
      return (Field) redactResult.getNode();
    } else {
      return origField;
    }
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

  public static Builder newQueryExecutorBatchLoader() {
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