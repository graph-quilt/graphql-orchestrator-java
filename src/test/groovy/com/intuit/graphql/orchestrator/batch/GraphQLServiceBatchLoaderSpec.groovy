package com.intuit.graphql.orchestrator.batch

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.authorization.BatchFieldAuthorization
import com.intuit.graphql.orchestrator.batch.GraphQLTestUtil.PassthroughQueryModifier
import com.intuit.graphql.orchestrator.schema.ServiceMetadataImpl
import graphql.GraphQLContext
import graphql.Scalars
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.ResultPath
import graphql.language.*
import graphql.language.OperationDefinition.Operation
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static com.intuit.graphql.orchestrator.batch.GraphQLServiceBatchLoader.newQueryExecutorBatchLoader
import static graphql.execution.MergedField.newMergedField
import static graphql.language.Field.newField
import static graphql.language.OperationDefinition.newOperationDefinition
import static graphql.language.TypeName.newTypeName
import static graphql.language.VariableDefinition.newVariableDefinition
import static graphql.language.VariableReference.newVariableReference
import static graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment
import static graphql.schema.GraphQLSchema.newSchema
import static java.util.Arrays.asList
import static java.util.Collections.emptyMap
import static java.util.Collections.singletonList

class GraphQLServiceBatchLoaderSpec extends Specification {

    static final GraphQLObjectType FIRST_TYPE = GraphQLObjectType.newObject().name("FirstType")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("s1").type(Scalars.GraphQLString)
                    .build())
            .build()

    static final GraphQLObjectType SECOND_TYPE = GraphQLObjectType.newObject().name("SecondType")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("s2").type(Scalars.GraphQLString)
                    .build())
            .build()

    public ServiceProvider mockServiceProvider

    public ServiceMetadataImpl mockServiceMetadata

    public VariableDefinitionFilter mockVariableDefinitionFilter

    public BatchFieldAuthorization mockBatchFieldAuthorization

    void setup() {
        mockServiceProvider = Mock(ServiceProvider)
        mockServiceMetadata = Mock(ServiceMetadataImpl)
        mockVariableDefinitionFilter = Mock(VariableDefinitionFilter)
        mockBatchFieldAuthorization = Mock(BatchFieldAuthorization)

        mockServiceMetadata.requiresTypenameInjection() >> false
        mockVariableDefinitionFilter.getVariableReferencesFromNode(
                _ as GraphQLSchema, _ as GraphQLObjectType, _ as Map, _ as Map, _ as Node) >> Collections.emptySet()

        mockBatchFieldAuthorization.getFutureAuthData() >> CompletableFuture.completedFuture("TestFutureAuthData")

        mockServiceProvider.isFederationProvider() >> false
        mockServiceMetadata.getServiceProvider() >> mockServiceProvider
    }

    void makesCorrectBatchQuery() {
        given:
        QueryExecutor validator = { environment, context ->
            //  TODO: add describedAs
            //    assertThat(environment.getVariables()).describedAs("Batch Loader merges variables")
            def vars = environment.getVariables()
            assert vars.get("1") == "2"
            assert vars.get("3") == "4"

            assert environment.getQuery() ==~ /query .*first second/

            assert environment.getOperationName() == "QUERY"
            assert environment.getRoot() != null

            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .field({ builder -> builder.name("second").type(SECOND_TYPE) })
                .build()

        MergedField mergedField1 = newMergedField(newField("first").build()).build()
        MergedField mergedField2 = newMergedField(newField("second").build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField1)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField1)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("2", "4"))
                .context(GraphQLContext.newContext().build())
                .graphQLSchema(graphQLSchema)
                .mergedField(mergedField2)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/second"))
                        .field(mergedField2)
                        .type(SECOND_TYPE)
                        .build())
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(asList(dfe1, dfe2))

        then:
        noExceptionThrown()
    }

    void makesCorrectBatchQueryWithCustomFieldAuthorization() {
        given:
        QueryExecutor validator = { environment, context ->
            //  TODO: add describedAs
            //   assertThat(environment.getVariables()).describedAs("Batch Loader merges variables")
            def vars = environment.getVariables()
            assert vars.get("1") == "2"
            assert vars.get("3") == "4"

            assert environment.getQuery() ==~ /query .*first second/
            assert environment.getOperationName() == "QUERY"
            assert environment.getRoot() != null

            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .field({ builder -> builder.name("second").type(SECOND_TYPE) })
                .build()

        MergedField mergedField1 = newMergedField(newField("first").build()).build()
        MergedField mergedField2 = newMergedField(newField("second").build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        GraphQLContext graphQLContext = GraphQLContext.newContext().build()
        graphQLContext.put(BatchFieldAuthorization.class, mockBatchFieldAuthorization)

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(graphQLContext)
                .mergedField(mergedField1)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField1)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("2", "4"))
                .context(GraphQLContext.newContext().build())
                .graphQLSchema(graphQLSchema)
                .mergedField(mergedField2)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/second"))
                        .field(mergedField2)
                        .type(SECOND_TYPE)
                        .build())
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(asList(dfe1, dfe2))

        then:
        noExceptionThrown()
    }

    void makesCorrectBatchMutation() {
        given:
        QueryExecutor validator = { environment, context ->
            assert environment.getQuery() ==~ /mutation .*first/
            assert environment.getOperationName() == "MUTATION"
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType mutationType = GraphQLObjectType.newObject().name("Mutation")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(GraphQLObjectType.newObject().name("Query")
                        .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                        .build())
                .mutation(mutationType).build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        OperationDefinition opDef = newOperationDefinition().operation(Operation.MUTATION).build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField)
                .parentType(mutationType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .operationDefinition(opDef)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void makesCorrectBatchMutationWithCustomFieldAuthorization() {
        given:
        QueryExecutor validator = { environment, context ->
            assert environment.getQuery() ==~ /mutation .*first/
            assert environment.getOperationName() == "MUTATION"
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType mutationType = GraphQLObjectType.newObject().name("Mutation")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(GraphQLObjectType.newObject().name("Query")
                        .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                        .build())
                .mutation(mutationType).build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        OperationDefinition opDef = newOperationDefinition().operation(Operation.MUTATION).build()

        GraphQLContext graphQLContext = GraphQLContext.newContext().build()
        graphQLContext.put(BatchFieldAuthorization.class, mockBatchFieldAuthorization)

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(graphQLContext)
                .mergedField(mergedField)
                .parentType(mutationType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .operationDefinition(opDef)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void defaultsOperationTypeToQuery() {
        given:
        QueryExecutor validator = { environment, context ->
            assert environment.getQuery() ==~ /query .*first/
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .mergedField(mergedField)
                .context(GraphQLContext.newContext().build())
                .parentType(GraphQLObjectType.newObject().name("somerandomtype").build())
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void propagatesQueryOperationName() {
        given:
        QueryExecutor validator = { environment, context ->
            assert environment.getQuery() ==~ /query/
            assert environment.getOperationName() == "FirstQuery"
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        OperationDefinition opDef = newOperationDefinition()
                .name("FirstQuery").operation(Operation.QUERY).build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .mergedField(mergedField)
                .context(GraphQLContext.newContext().build())
                .parentType(GraphQLObjectType.newObject().name("somerandomtype").build())
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .operationDefinition(opDef)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void propagatesMutationOperationName() {
        given:
        QueryExecutor validator = { environment, context ->
            assert environment.getQuery() ==~ /mutation/
            assert environment.getOperationName() == "DoFirst"
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType mutationType = GraphQLObjectType.newObject().name("Mutation")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("Query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType)
                .mutation(mutationType).build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        OperationDefinition opDef = newOperationDefinition()
                .name("DoFirst").operation(Operation.MUTATION).build()

        HashMap<String, Object> map = new HashMap<>()
        map.put("1", "3")
        map.put("2", null)
        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(map)
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField)
                .parentType(mutationType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .operationDefinition(opDef)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void propagatesVariableDefinitions() {
        given:
        mockVariableDefinitionFilter.getVariableReferencesFromNode(_ as GraphQLSchema, _ as GraphQLObjectType, _ as Map, _ as Map, _ as Node) >> new HashSet<String>() {{
            add("TestVariableDefinition")
            add("TestVariableDefinition2")
        }}

        QueryExecutor validator = { input, context ->
            assert input.getQuery() ==~ /Bulk_Query(\$TestVariableDefinition:TestType,\$TestVariableDefinition2:TestType2\)/
            assert input.getQuery() ==~ /fieldWithArgument\(SomeArgument:\$TestVariableDefinition\)/
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("fieldWithArgument").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        OperationDefinition operationWithVariableDefinitions = newOperationDefinition()
                .name("Bulk_Query")
                .variableDefinitions(asList(
                        newVariableDefinition("TestVariableDefinition")
                                .type(newTypeName("TestType").build())
                                .build(),
                        newVariableDefinition("TestVariableDefinition2")
                                .type(newTypeName("TestType2").build())
                                .build()
                )).operation(Operation.QUERY)
                .build()

        final MergedField mergedFieldWithArgument = newMergedField(
                newField("fieldWithArgument").arguments(
                        singletonList(Argument.newArgument("SomeArgument",
                                newVariableReference().name("TestVariableDefinition").build())
                                .build())).build()).build()

        final ExecutionStepInfo root = ExecutionStepInfo.newExecutionStepInfo()
                .type(GraphQLObjectType.newObject().name("FakeType").build())
                .path(ResultPath.rootPath())
                .build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(emptyMap())
                .graphQLSchema(graphQLSchema)
                .operationDefinition(operationWithVariableDefinitions)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedFieldWithArgument)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/fieldWithArgument"))
                        .parentInfo(root)
                        .field(mergedFieldWithArgument)
                        .type(FIRST_TYPE)
                        .build())
                .parentType(queryType)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(validator)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void testBuilderThrowsExceptionWithQueryExecutor() {
        given:
        GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader()

        builder.batchResultTransformer({ r, env -> null })
                .queryResponseModifier({ q -> null })
                .build()

        when:
        builder.queryExecutor(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderThrownExceptionWithQueryResponseModified() {
        given:
        GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader()

        builder.batchResultTransformer({ r, env -> null })
                .queryResponseModifier({ q -> null })
                .build()

        when:
        builder.queryResponseModifier(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderThrowsExceptionWithBatchResultTransformer() {
        given:
        GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader()

        builder.batchResultTransformer({ r, env -> null })
                .queryResponseModifier({ q -> null })
                .build()

        when:
        builder.batchResultTransformer(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderThrowsExceptionWithQueryOperationModifier() {
        given:
        GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader()

        builder.batchResultTransformer({ r, env -> null })
                .queryResponseModifier({ q -> null })
                .build()

        when:
        builder.queryOperationModifier(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderThrowsExceptionWithServiceMetadata() {
        given:
        GraphQLServiceBatchLoader.Builder builder = newQueryExecutorBatchLoader()

        builder.batchResultTransformer({ r, env -> null })
                .queryResponseModifier({ q -> null })
                .build()

        when:
        builder.serviceMetadata(null)

        then:
        thrown(NullPointerException)
    }

    void callsQueryModifierIfInterfaceFieldDefinitionExists() {
        given:
        QueryExecutor noopQueryExecutor = { executionInput, context ->
            assert executionInput.getOperationName() == "TestName"
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        mockServiceMetadata.requiresTypenameInjection() >> true

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("field").type(FIRST_TYPE) })
                .build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        OperationDefinition operationWithVariableDefinitions = newOperationDefinition().operation(Operation.QUERY)
                .build()

        final MergedField mergedField = newMergedField(newField("field").build())
                .build()

        final ExecutionStepInfo root = ExecutionStepInfo.newExecutionStepInfo()
                .type(GraphQLObjectType.newObject().name("FakeType").build())
                .path(ResultPath.rootPath())
                .build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(emptyMap())
                .graphQLSchema(graphQLSchema)
                .operationDefinition(operationWithVariableDefinitions)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/field"))
                        .parentInfo(root)
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .parentType(queryType)
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(noopQueryExecutor)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new QueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void queryDirectivesArePropagated() {
        given:
        QueryExecutor fn = { environment, context ->
            assert ((Document) environment.getRoot()).getDefinitionsOfType(OperationDefinition.class).get(0)
                    .getDirectives() != null
            return CompletableFuture.completedFuture(new HashMap<>())
        }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        MergedField mergedField = newMergedField(newField("first").build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        OperationDefinition operationDefinition = OperationDefinition.newOperationDefinition()
                .operation(Operation.QUERY)
                .directives(Collections.singletonList(Directive.newDirective().name("some_directive").build()))
                .build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField)
                .parentType(queryType)
                .operationDefinition(operationDefinition)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(fn)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(singletonList(dfe1))

        then:
        noExceptionThrown()
    }

    void variableFilterNotCalledWhenEmpty() {
        given:
        QueryExecutor fn = { environment, context -> CompletableFuture.completedFuture(new HashMap<>()) }

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .field({ builder -> builder.name("second").type(SECOND_TYPE) })
                .build()

        MergedField mergedField1 = newMergedField(newField("first").build()).build()
        MergedField mergedField2 = newMergedField(newField("second").build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField1)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField1)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("2", "4"))
                .context(GraphQLContext.newContext().build())
                .graphQLSchema(graphQLSchema)
                .mergedField(mergedField2)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/second"))
                        .field(mergedField2)
                        .type(SECOND_TYPE)
                        .build())
                .build()

        GraphQLServiceBatchLoader batchLoader = GraphQLServiceBatchLoader.newQueryExecutorBatchLoader()
                .queryExecutor(fn)
                .serviceMetadata(mockServiceMetadata)
                .queryOperationModifier(new PassthroughQueryModifier())
                .build()

        batchLoader.variableDefinitionFilter = mockVariableDefinitionFilter

        when:
        batchLoader.load(asList(dfe1, dfe2))

        then:
        0 * mockVariableDefinitionFilter.getVariableReferencesFromNode(_ as GraphQLSchema, _ as GraphQLObjectType, _ as Map, _ as Map, _ as Node)
    }

    void callsAllHooks() {
        given:
        QueryExecutor emptyFn = { input, context -> CompletableFuture.completedFuture(new HashMap<>()) }

        final BatchLoaderExecutionHooks mockHooks = Mock(BatchLoaderExecutionHooks.class)

        GraphQLServiceBatchLoader loader = newQueryExecutorBatchLoader()
                .queryExecutor(emptyFn)
                .serviceMetadata(mockServiceMetadata)
                .batchLoaderExecutionHooks(mockHooks)
                .build()

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .build()

        MergedField mergedField1 = newMergedField(newField("first").build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        DataFetchingEnvironment dfe = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .mergedField(mergedField1)
                .parentType(queryType)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .path(ResultPath.parse("/first"))
                        .field(mergedField1)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        when:
        loader.load(Collections.singletonList(dfe)).toCompletableFuture().join()

        then:
        1 * mockHooks.onBatchLoadStart(_, _)
        1 * mockHooks.onExecutionInput(_, _)
        1 * mockHooks.onQueryResult(_, _)
        1 * mockHooks.onBatchLoadEnd(_, _)
    }

    void fragmentDefinitionCallTest() {
        given:
        QueryExecutor emptyFn = { input, context -> CompletableFuture.completedFuture(new HashMap<>()) }
        final BatchLoaderExecutionHooks mockHooks = Mock(BatchLoaderExecutionHooks.class)
        GraphQLServiceBatchLoader loader = newQueryExecutorBatchLoader()
                .queryExecutor(emptyFn)
                .serviceMetadata(mockServiceMetadata)
                .batchLoaderExecutionHooks(mockHooks)
                .build()

        GraphQLObjectType queryType = GraphQLObjectType.newObject().name("query")
                .field({ builder -> builder.name("first").type(FIRST_TYPE) })
                .field({ builder -> builder.name("second").type(SECOND_TYPE) })
                .build()

        MergedField mergedField1 = newMergedField(newField("first").selectionSet(SelectionSet.newSelectionSet()
                .selection(FragmentSpread.newFragmentSpread("firstFrag").build())
                .selection(FragmentSpread.newFragmentSpread("secondFrag").build()).build()).build())
                .build()

        MergedField mergedField2 = newMergedField(newField("second").selectionSet(SelectionSet.newSelectionSet()
                .selection(FragmentSpread.newFragmentSpread("firstFrag").build())
                .selection(FragmentSpread.newFragmentSpread("secondFrag").build()).build()).build()).build()

        GraphQLSchema graphQLSchema = newSchema()
                .query(queryType).build()

        FragmentDefinition fdb1 =
                FragmentDefinition.newFragmentDefinition()
                        .name("firstFragDef")
                        .selectionSet(SelectionSet.newSelectionSet()
                                .selection(newField().name("first").build())
                                .selection(newField().name("second").build()).build())
                        .typeCondition(newTypeName().name("query").build())
                        .build()

        FragmentDefinition fdb2 =
                FragmentDefinition.newFragmentDefinition()
                        .name("secondFragDef")
                        .selectionSet(SelectionSet.newSelectionSet()
                                .selection(Field.newField().name("first").build())
                                .selection(Field.newField().name("second").build()).build())
                        .typeCondition(newTypeName().name("query").build())
                        .build()

        Map<String, FragmentDefinition> m = new HashMap()
        m.put("firstFrag", fdb1)
        m.put("secondFrag", fdb2)

        DataFetchingEnvironment dfe1 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .parentType(queryType)
                .fragmentsByName(m)
                .mergedField(mergedField2)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .parentInfo(ExecutionStepInfo.newExecutionStepInfo()
                                .path(ResultPath.parse(""))
                                .field(mergedField1)
                                .type(queryType)
                                .build())
                        .path(ResultPath.parse("/first"))
                        .field(mergedField1)
                        .type(FIRST_TYPE)
                        .build())
                .build()

        DataFetchingEnvironment dfe2 = newDataFetchingEnvironment()
                .variables(ImmutableMap.of("1", "3"))
                .graphQLSchema(graphQLSchema)
                .context(GraphQLContext.newContext().build())
                .parentType(queryType)
                .fragmentsByName(m)
                .mergedField(mergedField2)
                .executionStepInfo(ExecutionStepInfo.newExecutionStepInfo()
                        .parentInfo(ExecutionStepInfo.newExecutionStepInfo()
                                .path(ResultPath.parse(""))
                                .field(mergedField1)
                                .type(queryType)
                                .build())
                        .path(ResultPath.parse("/second"))
                        .field(mergedField2)
                        .type(SECOND_TYPE)
                        .build())
                .build()

        when:
        loader.load(asList(dfe1, dfe2)).whenComplete({ results, ex ->
            assert results.size() == 2
        }).toCompletableFuture().join()

        then:
        noExceptionThrown()
    }

    private static class QueryModifier extends QueryOperationModifier {

        @Override
        OperationDefinition modifyQuery(
                final GraphQLSchema graphQLSchema,
                final OperationDefinition operationDefinition,
                final Map<String, FragmentDefinition> fragmentsByName,
                final Map<String, Object> variables) {
            return operationDefinition.transform(
                    { builder -> builder.name("TestName") })
        }
    }
}