package com.intuit.graphql.orchestrator.batch;

import com.intuit.graphql.orchestrator.authorization.BatchFieldAuthorization;
import com.intuit.graphql.orchestrator.authorization.DefaultBatchFieldAuthorization;
import com.intuit.graphql.orchestrator.batch.MergedFieldModifier.MergedFieldModifierResult;
import com.intuit.graphql.orchestrator.schema.GraphQLObjects;
import com.intuit.graphql.orchestrator.schema.ServiceMetadata;
import static com.intuit.graphql.orchestrator.schema.transform.DomainTypesTransformer.DELIMITER;
import static com.intuit.graphql.orchestrator.utils.GraphQLUtil.AST_TRANSFORMER;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.VisibleForTesting;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import static graphql.language.AstPrinter.printAstCompact;
import graphql.language.Definition;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import static graphql.language.OperationDefinition.Operation.QUERY;
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
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static java.util.Objects.requireNonNull;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GraphQLServiceBatchLoader implements BatchLoader<DataFetchingEnvironment, DataFetcherResult<Object>> {

  private final BatchFieldAuthorization DEFAULT_FIELD_AUTHORIZATION = new DefaultBatchFieldAuthorization();
  private final QueryExecutor queryExecutor;
  private final QueryResponseModifier queryResponseModifier;
  private final BatchResultTransformer batchResultTransformer;
  private final QueryOperationModifier queryOperationModifier;
  private final ServiceMetadata serviceMetadata;
  private final BatchLoaderExecutionHooks<DataFetchingEnvironment, DataFetcherResult<Object>> hooks;

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
    BatchFieldAuthorization batchFieldAuthorization = getDefaultOrCustomFieldAuthorization(graphQLContext);
    CompletableFuture<Object> futureAuthData = batchFieldAuthorization.getFutureAuthData();
    return futureAuthData.thenCompose(authData -> load(keys, graphQLContext, authData,
        batchFieldAuthorization));
  }

  private BatchFieldAuthorization getDefaultOrCustomFieldAuthorization(GraphQLContext graphQLContext) {
    BatchFieldAuthorization ctxBatchFieldAuthorization = graphQLContext.get(BatchFieldAuthorization.class);
    return Objects.isNull(ctxBatchFieldAuthorization)
        ? DEFAULT_FIELD_AUTHORIZATION : ctxBatchFieldAuthorization;
  }

  private CompletableFuture<List<DataFetcherResult<Object>>> load(List<DataFetchingEnvironment> keys,
      GraphQLContext context, Object authData, BatchFieldAuthorization batchFieldAuthorization) {

    batchFieldAuthorization.batchAuthorizeOrThrowGraphQLError(authData, keys);
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
          .map(field -> (serviceMetadata.shouldModifyDownStreamQuery())
            ? removeFieldsWithExternalTypes(field, getRootFieldDefinition(key.getExecutionStepInfo()).getType(),
                key.getFragmentsByName(), graphQLSchema)
            : field)
          .forEach(selectionSetBuilder :: selection);
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
      if (serviceMetadata.hasFieldResolverDirective() || serviceMetadata.isFederationService()) {
        Map<String, FragmentDefinition> finalServiceFragmentDefinitions = new HashMap<>();
        svcFragmentDefinitions.forEach((fragmentName, fragmentDefinition) -> {
          String typeConditionName = fragmentDefinition.getTypeCondition().getName();
          GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) graphQLSchema.getType(typeConditionName);
          FragmentDefinition transformedFragment = removeFieldsWithExternalTypes(fragmentDefinition,
              parentType, key.getFragmentsByName(), graphQLSchema);
          finalServiceFragmentDefinitions.put(fragmentName, removeDomainTypeFromFragment(transformedFragment));
        });
        mergedFragmentDefinitions.putAll(finalServiceFragmentDefinitions);
      } else {
        mergedFragmentDefinitions.putAll(svcFragmentDefinitions);
      }
    }

    SelectionSet filteredSelection = selectionSetBuilder.build();
    if(operationType.name().equalsIgnoreCase("QUERY")
            && canOptimiseQuery(filteredSelection)) {
      SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();

      HashMap<String, Set<Field>> selectionSetTree = new HashMap<>();
      createSelectionSetTree(filteredSelection, selectionSetTree);
//      List<Field> distinctRoots = filteredSelection.getSelections().stream()
//              .map( rootNode -> (Field) rootNode).filter(distinctByFieldName(Field:: getName))
//              .collect(Collectors.toList());
//      distinctRoots.stream()
//              //.map( rootNode -> (Field) rootNode)
//              .forEach( rootNode -> mergeFilteredSelection(rootNode.getName(), selectionSetTree)
//                      .getSelections().stream().forEach(mergedSelectionSetBuilder ::selection));

      filteredSelection.getSelections().stream()
              .map( rootNode -> (Field) rootNode).filter(distinctByFieldName(Field:: getName))
              .forEach( rootNode -> mergeFilteredSelection(rootNode, selectionSetTree)
                      .getSelections().stream().forEach(mergedSelectionSetBuilder ::selection));
      SelectionSet mergedFilterSelection = mergedSelectionSetBuilder.build();
      filteredSelection = mergedFilterSelection;

    }
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
        .thenApply(batchResult -> {
          hooks.onBatchLoadEnd(context, batchResult);
          return batchResult;
        });
  }
  public static <T> Predicate<T> distinctByFieldName(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private boolean canOptimiseQuery(SelectionSet selectionSet){
    if(selectionSet == null) return true;
    for( Selection selection : selectionSet.getSelections()){
      if(selection.getClass() != Field.class
              || !canOptimiseQuery(((Field) selection).getSelectionSet()))
        return false;
    }
    return true;
  }
  private SelectionSet mergeFilteredSelection(Field node, HashMap<String, Set<Field>> selectionSetTree){
    Field field;
    SelectionSet.Builder selectionSetBuilder = SelectionSet.newSelectionSet();
    SelectionSet.Builder childSelectionSetBuilder = SelectionSet.newSelectionSet();

    if (ObjectUtils.isEmpty(selectionSetTree.get(node.getName()))) { // leaf node
      field = new Field(node.getName(), node.getArguments());

      selectionSetBuilder.selection(field);
      SelectionSet set = selectionSetBuilder.build();
      return set;
    }
    List<SelectionSet> childrenSets = new ArrayList<>();
    for( Field f : selectionSetTree.get(node.getName())) {
      childrenSets.add(mergeFilteredSelection(f, selectionSetTree));
    }
    childrenSets
            .stream()
            .map(s -> s.getSelections())
            .flatMap(Collection::stream)
            .collect(Collectors.toList())
            .stream()
            .forEach(childSelectionSetBuilder::selection);
    field = new Field(node.getName(), node.getArguments(), childSelectionSetBuilder.build());
    selectionSetBuilder.selection(field);

    return selectionSetBuilder.build() ;
  }

  private void createSelectionSetTree(SelectionSet selectionSet, HashMap<String, Set<Field>> selectionSetMap){
    selectionSet.getSelections().stream().forEach(field -> {
      Field f = (Field) field;
      if(f.getSelectionSet() != null){ // leaf node
        if(selectionSetMap.containsKey(f.getName()) ) {
          selectionSetMap.get(f.getName())
                  .addAll((f.getSelectionSet().getSelections()
                          .stream()
                          .map(s ->(Field) s)
                          .collect(Collectors.toList())));
        }
        else selectionSetMap.put(f.getName(), new HashSet<>(
                f.getSelectionSet().getSelections()
                        .stream()
                        .map(s -> (Field)s)
                        .collect(Collectors.toList())));
        createSelectionSetTree(f.getSelectionSet(), selectionSetMap);
      }
    });
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

  /**
   * remove fields with external type from a fragment definition.
   *
   * @param origFragmentDefinition original fragment definition
   * @param typeCondition type condition of the original fragment definition.  This will be used as
   *                      the root type for {@link DownstreamQueryModifier}
   * @return a modified fragment definition
   */
  private FragmentDefinition removeFieldsWithExternalTypes(final FragmentDefinition origFragmentDefinition,
      GraphQLType typeCondition, Map<String, FragmentDefinition> fragmentsByName, GraphQLSchema graphQLSchema) {
    // call serviceMetadata.hasFieldResolverDirective() before calling this method
    return (FragmentDefinition) AST_TRANSFORMER.transform(origFragmentDefinition,
        new DownstreamQueryModifier(unwrapAll(typeCondition), serviceMetadata, fragmentsByName, graphQLSchema));

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
   * DownstreamQueryModifier}
   * @return a modified field
   */
  private Field removeFieldsWithExternalTypes(Field origField, GraphQLType fieldType, Map<String, FragmentDefinition> fragmentsByName,
      GraphQLSchema graphQLSchema) {
    // call serviceMetadata.hasFieldResolverDirective() before calling this method
    return (Field) AST_TRANSFORMER.transform(origField,
        new DownstreamQueryModifier(unwrapAll(fieldType), serviceMetadata, fragmentsByName, graphQLSchema));
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