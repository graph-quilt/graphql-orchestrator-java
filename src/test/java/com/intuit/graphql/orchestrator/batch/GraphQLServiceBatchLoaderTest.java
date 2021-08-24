//package com.intuit.graphql.orchestrator.batch;
//
//import static com.intuit.graphql.orchestrator.batch.GraphQLServiceBatchLoader.newQueryExecutorBatchLoader;
//import static graphql.execution.MergedField.newMergedField;
//import static graphql.language.Field.newField;
//import static graphql.language.OperationDefinition.newOperationDefinition;
//import static graphql.language.TypeName.newTypeName;
//import static graphql.language.VariableDefinition.newVariableDefinition;
//import static graphql.language.VariableReference.newVariableReference;
//import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment;
//import static graphql.schema.GraphQLSchema.newSchema;
//import static java.util.Arrays.asList;
//import static java.util.Collections.emptyMap;
//import static java.util.Collections.singletonList;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyMap;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.MockitoAnnotations.initMocks;
//
//import com.google.common.collect.ImmutableMap;
//import com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.PassthroughQueryModifier;
//import com.intuit.graphql.orchestrator.xtext.XtextGraph;
//import graphql.GraphQLContext;
//import graphql.Scalars;
//import graphql.execution.ExecutionPath;
//import graphql.execution.ExecutionStepInfo;
//import graphql.execution.MergedField;
//import graphql.language.Argument;
//import graphql.language.Directive;
//import graphql.language.Document;
//import graphql.language.Field;
//import graphql.language.FragmentDefinition;
//import graphql.language.FragmentSpread;
//import graphql.language.Node;
//import graphql.language.OperationDefinition;
//import graphql.language.OperationDefinition.Operation;
//import graphql.language.SelectionSet;
//import graphql.schema.DataFetchingEnvironment;
//import graphql.schema.GraphQLObjectType;
//import graphql.schema.GraphQLSchema;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//
//public class GraphQLServiceBatchLoaderTest {
//
//  @Mock
//  public XtextGraph mockServiceMetadata;
//
//  @Mock
//  public VariableDefinitionFilter mockVariableDefinitionFilter;
//
//  @Before
//  public void setUp() {
//    initMocks(this);
//    doReturn(false).when(mockServiceMetadata).requiresTypenameInjection();
//    doReturn(Collections.emptySet()).when(mockVariableDefinitionFilter)
//        .getVariableReferencesFromNode(any(GraphQLSchema.class), any(GraphQLObjectType.class), anyMap(), anyMap(),
//            any(Node.class));
//  }
//
//  @Test
//  public void makesCorrectBatchQuery() {
//    QueryExecutor validator = (environment, context) -> {
//      assertThat(environment.getVariables())
//          .describedAs("Batch Loader merges variables")
//          .extracting("1", "2")
//          .containsOnly("3", "4");
//
//      assertThat(environment.getQuery()).contains("query", "first", "second");
//      assertThat(environment.getOperationName()).isEqualTo("QUERY");
//      assertThat(environment.getRoot()).isNotNull();
//
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    MergedField mergedField1 = newMergedField(newField("first").build()).build();
//    MergedField mergedField2 = newMergedField(newField("second").build()).build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField1)
//        .parentType(queryType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField1)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("2", "4"))
//        .context(GraphQLContext.newContext().build())
//        .graphQLSchema(graphQLSchema)
//        .mergedField(mergedField2)
//        .parentType(queryType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/second"))
//            .field(mergedField2)
//            .type(GraphQLObjectType.newObject().name("SecondType").build())
//            .build())
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(asList(dfe1, dfe2));
//  }
//
//  @Test
//  public void makesCorrectBatchMutation() {
//
//    QueryExecutor validator = (environment, context) -> {
//      assertThat(environment.getQuery()).contains("mutation", "first");
//      assertThat(environment.getOperationName()).isEqualTo("MUTATION");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType mutationType = GraphQLObjectType.newObject().name("Mutation").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(GraphQLObjectType.newObject().name("Query")
//            .field(builder -> builder.name("first").type(Scalars.GraphQLInt))
//            .build())
//        .mutation(mutationType).build();
//
//    MergedField mergedField = newMergedField(newField("first").build()).build();
//
//    OperationDefinition opDef = newOperationDefinition().operation(Operation.MUTATION).build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField)
//        .parentType(mutationType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .operationDefinition(opDef)
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void defaultsOperationTypeToQuery() {
//
//    QueryExecutor validator = (environment, context) -> {
//      assertThat(environment.getQuery()).contains("query", "first");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    MergedField mergedField = newMergedField(newField("first").build()).build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .mergedField(mergedField)
//        .context(GraphQLContext.newContext().build())
//        .parentType(GraphQLObjectType.newObject().name("somerandomtype").build())
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void propagatesQueryOperationName() {
//
//    QueryExecutor validator = (environment, context) -> {
//      assertThat(environment.getQuery()).contains("query");
//      assertThat(environment.getOperationName()).isEqualTo("FirstQuery");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    MergedField mergedField = newMergedField(newField("first").build()).build();
//
//    OperationDefinition opDef = newOperationDefinition()
//        .name("FirstQuery").operation(Operation.QUERY).build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .mergedField(mergedField)
//        .context(GraphQLContext.newContext().build())
//        .parentType(GraphQLObjectType.newObject().name("somerandomtype").build())
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .operationDefinition(opDef)
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void propagatesMutationOperationName() {
//
//    QueryExecutor validator = (environment, context) -> {
//      assertThat(environment.getQuery()).contains("mutation");
//      assertThat(environment.getOperationName()).isEqualTo("DoFirst");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType mutationType = GraphQLObjectType.newObject().name("Mutation").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(GraphQLObjectType.newObject().name("Query").build())
//        .mutation(mutationType).build();
//
//    MergedField mergedField = newMergedField(newField("first").build()).build();
//
//    OperationDefinition opDef = newOperationDefinition()
//        .name("DoFirst").operation(Operation.MUTATION).build();
//
//    HashMap<String, Object> map = new HashMap<>();
//    map.put("1", "3");
//    map.put("2", null);
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(map)
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField)
//        .parentType(mutationType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .operationDefinition(opDef)
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void propagatesVariableDefinitions() {
//
//    doReturn(new HashSet<String>() {{
//      add("TestVariableDefinition");
//      add("TestVariableDefinition2");
//    }})
//        .when(mockVariableDefinitionFilter)
//        .getVariableReferencesFromNode(any(GraphQLSchema.class), any(GraphQLObjectType.class), anyMap(), anyMap(),
//            any(Node.class));
//    QueryExecutor validator = (input, context) -> {
//      assertThat(input.getQuery())
//          .contains("Bulk_Query($TestVariableDefinition:TestType,$TestVariableDefinition2:TestType2)")
//          .contains("fieldWithArgument(SomeArgument:$TestVariableDefinition)");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    OperationDefinition operationWithVariableDefinitions = newOperationDefinition()
//        .name("Bulk_Query")
//        .variableDefinitions(asList(
//            newVariableDefinition("TestVariableDefinition")
//                .type(newTypeName("TestType").build())
//                .build(),
//            newVariableDefinition("TestVariableDefinition2")
//                .type(newTypeName("TestType2").build())
//                .build()
//        )).operation(Operation.QUERY)
//        .build();
//
//    final MergedField mergedFieldWithArgument = newMergedField(
//        newField("fieldWithArgument").arguments(
//            singletonList(Argument.newArgument("SomeArgument",
//                newVariableReference().name("TestVariableDefinition").build())
//                .build())).build()).build();
//
//    final ExecutionStepInfo root = ExecutionStepInfo.newExecutionStepInfo()
//        .type(GraphQLObjectType.newObject().name("FakeType").build())
//        .path(ExecutionPath.rootPath())
//        .build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(emptyMap())
//        .graphQLSchema(graphQLSchema)
//        .operationDefinition(operationWithVariableDefinitions)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedFieldWithArgument)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/fieldWithArgument"))
//            .parentInfo(root)
//            .field(mergedFieldWithArgument)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .parentType(queryType)
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(validator)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//
//  @Test
//  public void testBuilder() {
//    GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader();
//
//    assertThatThrownBy(() -> builder.queryExecutor(null)).isInstanceOf(NullPointerException.class);
//    assertThatThrownBy(() -> builder.queryResponseModifier(null)).isInstanceOf(NullPointerException.class);
//    assertThatThrownBy(() -> builder.batchResultTransformer(null)).isInstanceOf(NullPointerException.class);
//    assertThatThrownBy(() -> builder.queryOperationModifier(null)).isInstanceOf(NullPointerException.class);
//    assertThatThrownBy(() -> builder.serviceMetadata(null)).isInstanceOf(NullPointerException.class);
//
//    builder.batchResultTransformer((r, env) -> null)
//        .queryResponseModifier(q -> null)
//        .build();
//  }
//
//  @Test
//  public void callsQueryModifierIfInterfaceFieldDefinitionExists() {
//    QueryExecutor noopQueryExecutor = (executionInput, context) -> {
//      assertThat(executionInput.getOperationName()).isEqualTo("TestName");
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    doReturn(true).when(mockServiceMetadata).requiresTypenameInjection();
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    OperationDefinition operationWithVariableDefinitions = newOperationDefinition().operation(Operation.QUERY)
//        .build();
//
//    final MergedField mergedField = newMergedField(newField("field").build())
//        .build();
//
//    final ExecutionStepInfo root = ExecutionStepInfo.newExecutionStepInfo()
//        .type(GraphQLObjectType.newObject().name("FakeType").build())
//        .path(ExecutionPath.rootPath())
//        .build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(emptyMap())
//        .graphQLSchema(graphQLSchema)
//        .operationDefinition(operationWithVariableDefinitions)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/field"))
//            .parentInfo(root)
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .parentType(queryType)
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(noopQueryExecutor)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new QueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void queryDirectivesArePropagated() {
//    QueryExecutor fn = (environment, context) -> {
//      assertThat(
//          ((Document) environment.getRoot()).getDefinitionsOfType(OperationDefinition.class).get(0).getDirectives())
//          .isNotEmpty();
//      return CompletableFuture.completedFuture(new HashMap<>());
//    };
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    MergedField mergedField = newMergedField(newField("first").build()).build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    OperationDefinition operationDefinition = OperationDefinition.newOperationDefinition()
//        .operation(Operation.QUERY)
//        .directives(Collections.singletonList(Directive.newDirective().name("some_directive").build()))
//        .build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField)
//        .parentType(queryType)
//        .operationDefinition(operationDefinition)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(fn)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(singletonList(dfe1));
//  }
//
//  @Test
//  public void variableFilterNotCalledWhenEmpty() {
//    QueryExecutor fn = (environment, context) -> CompletableFuture.completedFuture(new HashMap<>());
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    MergedField mergedField1 = newMergedField(newField("first").build()).build();
//    MergedField mergedField2 = newMergedField(newField("second").build()).build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField1)
//        .parentType(queryType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField1)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("2", "4"))
//        .context(GraphQLContext.newContext().build())
//        .graphQLSchema(graphQLSchema)
//        .mergedField(mergedField2)
//        .parentType(queryType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/second"))
//            .field(mergedField2)
//            .type(GraphQLObjectType.newObject().name("SecondType").build())
//            .build())
//        .build();
//
//    GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
//        .queryExecutor(fn)
//        .serviceMetadata(mockServiceMetadata)
//        .queryOperationModifier(new PassthroughQueryModifier())
//        .build();
//
//    batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter;
//
//    batchLoader.load(asList(dfe1, dfe2));
//
//    Mockito.verify(mockVariableDefinitionFilter, never())
//        .getVariableReferencesFromNode(any(GraphQLSchema.class), any(GraphQLObjectType.class), anyMap(), anyMap(),
//            any(Node.class));
//  }
//
//  @Test
//  public void callsAllHooks() {
//    QueryExecutor emptyFn = (input, context) -> CompletableFuture.completedFuture(new HashMap<>());
//    final BatchLoaderExecutionHooks mockHooks = mock(BatchLoaderExecutionHooks.class);
//    GraphQLServiceBatchLoader loader = newQueryExecutorBatchLoader()
//        .queryExecutor(emptyFn)
//        .serviceMetadata(mockServiceMetadata)
//        .batchLoaderExecutionHooks(mockHooks)
//        .build();
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    MergedField mergedField1 = newMergedField(newField("first").build()).build();
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    DataFetchingEnvironment dfe = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .mergedField(mergedField1)
//        .parentType(queryType)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .path(ExecutionPath.parse("/first"))
//            .field(mergedField1)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    loader.load(Collections.singletonList(dfe)).whenComplete((not, used) -> {
//      verify(mockHooks, times(1)).onBatchLoadStart(any(), any());
//      verify(mockHooks, times(1)).onExecutionInput(any(), any());
//      verify(mockHooks, times(1)).onQueryResult(any(), any());
//      verify(mockHooks, times(1)).onBatchLoadEnd(any(), any());
//    }).toCompletableFuture().join();
//
//  }
//
//  @Test
//  public void fragmentDefinitionCallTest() {
//    QueryExecutor emptyFn = (input, context) -> CompletableFuture.completedFuture(new HashMap<>());
//    final BatchLoaderExecutionHooks mockHooks = mock(BatchLoaderExecutionHooks.class);
//    GraphQLServiceBatchLoader loader = newQueryExecutorBatchLoader()
//        .queryExecutor(emptyFn)
//        .serviceMetadata(mockServiceMetadata)
//        .batchLoaderExecutionHooks(mockHooks)
//        .build();
//
//    GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query").build();
//
//    MergedField mergedField1 = newMergedField(newField("first").selectionSet(SelectionSet.newSelectionSet()
//        .selection(FragmentSpread.newFragmentSpread("firstFrag").build())
//        .selection(FragmentSpread.newFragmentSpread("secondFrag").build()).build()).build())
//        .build();
//
//    MergedField mergedField2 = newMergedField(newField("second").selectionSet(SelectionSet.newSelectionSet()
//        .selection(FragmentSpread.newFragmentSpread("firstFrag").build())
//        .selection(FragmentSpread.newFragmentSpread("secondFrag").build()).build()).build()).build();
//
//
//    GraphQLSchema graphQLSchema = newSchema()
//        .query(queryType).build();
//
//    FragmentDefinition fdb1 =
//        FragmentDefinition.newFragmentDefinition()
//          .name("firstFragDef")
//          .selectionSet(SelectionSet.newSelectionSet()
//              .selection(newField().name("first").build())
//              .selection(newField().name("second").build()).build())
//            .typeCondition(newTypeName().name("firstType").build())
//            .build();
//
//    FragmentDefinition fdb2 =
//        FragmentDefinition.newFragmentDefinition()
//            .name("secondFragDef")
//            .selectionSet(SelectionSet.newSelectionSet()
//                .selection(Field.newField().name("first").build())
//                .selection(Field.newField().name("second").build()).build())
//            .typeCondition(newTypeName().name("secondType").build())
//            .build();
//
//    Map<String, FragmentDefinition> m = new HashMap();
//    m.put("firstFrag", fdb1);
//    m.put("secondFrag", fdb2);
//
//
//    DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .parentType(queryType)
//        .fragmentsByName(m)
//        .mergedField(mergedField2)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .parentInfo(ExecutionStepInfo.newExecutionStepInfo()
//                .path(ExecutionPath.parse("/first"))
//                .field(mergedField1)
//                .type(GraphQLObjectType.newObject().name("ParentType").build())
//                .build())
//            .path(ExecutionPath.parse("/first/second"))
//            .field(mergedField2)
//            .field(mergedField2)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
//        .variables(ImmutableMap.of("1", "3"))
//        .graphQLSchema(graphQLSchema)
//        .context(GraphQLContext.newContext().build())
//        .parentType(queryType)
//        .fragmentsByName(m)
//        .mergedField(mergedField2)
//        .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
//            .parentInfo(ExecutionStepInfo.newExecutionStepInfo()
//                .path(ExecutionPath.parse("/first"))
//                .field(mergedField1)
//                .type(GraphQLObjectType.newObject().name("ParentType").build())
//                .build())
//            .path(ExecutionPath.parse("/first/second"))
//            .field(mergedField2)
//            .field(mergedField2)
//            .type(GraphQLObjectType.newObject().name("FirstType").build())
//            .build())
//        .build();
//
//    loader.load(asList(dfe1, dfe2)).whenComplete((results, ex) -> {
//      assertThat(results.size() == 2);
//    }).toCompletableFuture().join();
//  }
//
//  private static class QueryModifier extends QueryOperationModifier {
//
//    @Override
//    public OperationDefinition modifyQuery(final GraphQLSchema graphQLSchema,
//        final OperationDefinition operationDefinition,
//        final Map<String, FragmentDefinition> fragmentsByName, final Map<String, Object> variables) {
//      return operationDefinition.transform(builder -> builder.name("TestName"));
//    }
//  }
//}