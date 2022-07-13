package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective
import graphql.Scalars
import graphql.execution.DataFetcherResult
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.ResultPath
import graphql.language.Argument
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.VariableReference
import graphql.schema.*
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class ResolverArgumentDataFetcherHelperSpec extends Specification {

    String schema = '''
        schema { query: QueryType }
        type QueryType { test_field: Int container: Container }
        type Container { test_field: Int }
    '''

    public DataLoader<DataFetchingEnvironment, DataFetcherResult<Object>> mockDataLoader

    private GraphQLSchema graphQLSchema

    private ResolverArgumentDataFetcherHelper dataFetcherHelper

    private static final String namespace = "test_namespace"

    private DataLoaderRegistry dataLoaderRegistry

    def setup() {
        mockDataLoader = Mock(DataLoader.class)

        graphQLSchema = TestHelper.schema(schema)
        dataFetcherHelper = new ResolverArgumentDataFetcherHelper(namespace)
        dataLoaderRegistry = new DataLoaderRegistry()
        dataLoaderRegistry.register(namespace, mockDataLoader)
    }

    def "single Argument Success"() {
        given:
        String query = "{ test_field }"
        String argumentName = "arg"
        String expectedArgumentReferenceName = "arg_0"

        mockDataLoader.load(_) >> { DataFetchingEnvironment environment ->
            assert ((String) environment.getArgument(argumentName)) == "test_value"
            def argument = environment.getMergedField().getSingleField().getArguments().get(0)
            assert argument.getName() == argumentName
            assert ((VariableReference) argument.getValue()).getName() == expectedArgumentReferenceName

            def stepArg = environment.getExecutionStepInfo().getField().getSingleField().getArguments().get(0)
            assert stepArg.getName() == argumentName
            assert ((VariableReference) argument.getValue()).getName() == expectedArgumentReferenceName

            assert environment.getExecutionStepInfo().<VariableReference>getArgument(argumentName).getName() == expectedArgumentReferenceName

            return CompletableFuture.completedFuture(DataFetcherResult.newResult().data("Huzzah!").build())
        }

        final OperationDefinition queryOperation = TestHelper.query(query)
        final Field testFieldNameField = (Field) queryOperation.getSelectionSet().getSelections().get(0)
        final GraphQLObjectType queryType = graphQLSchema.getQueryType()

        Map<ResolverArgumentDirective, Object> arguments = new HashMap<>()

        final ResolverArgumentDirective resolverArgumentDirective = ResolverArgumentDirective.newBuilder()
                .field(testFieldNameField.getName())
                .argumentName(argumentName)
                .graphQLInputType(GraphQLInputObjectType.newInputObject().name("Test_Input_Object_Type").build())
                .build()

        arguments.put(resolverArgumentDirective, "test_value")

        GraphQLOutputType type = Scalars.GraphQLInt

        final ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(type)
                .field(MergedField.newMergedField(testFieldNameField).build())
                .path(ResultPath.parse("/test_field"))
                .arguments(Collections.emptyMap())
                .fieldDefinition(queryType.getFieldDefinition(testFieldNameField.getName()))
                .fieldContainer(queryType)
                .build()

        DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(MergedField.newMergedField(testFieldNameField).build())
                .executionStepInfo(executionStepInfo)
                .graphQLSchema(graphQLSchema)
                .variables(Collections.emptyMap())
                .operationDefinition(queryOperation)
                .dataLoaderRegistry(dataLoaderRegistry)
                .fieldDefinition(queryType.getFieldDefinition(testFieldNameField.getName()))
                .arguments(Collections.emptyMap())
                .build()

        when:
        final DataFetcherResult<Object> result = dataFetcherHelper.callBatchLoaderWithArguments(env, arguments).join()

        then:
        result.getData() == "Huzzah!"
    }

    def "multiple Arguments"() {
        given:
        String query = "{ container { test_field } }"
        String arg1 = "arg_1"
        String arg2 = "arg_2"

        String argValue = "test_value"

        mockDataLoader.load(_) >> { DataFetchingEnvironment environment ->
            assert ((String) environment.getArgument(arg1)) == "test_value"

            Map<String, String> map = asArgumentMap(environment.getMergedField().getSingleField().getArguments())
            assert map.size() == 2
            assert map.keySet().containsAll(arg1, arg2)
            assert map.get(arg1).matches("arg_1_\\d")
            assert map.get(arg2).matches("arg_2_\\d")

            Map<String, String> stepMap = asArgumentMap(environment.getExecutionStepInfo().getField().getSingleField().getArguments())
            assert stepMap.size() == 2
            assert stepMap.keySet().containsAll(arg1, arg2)
            assert stepMap.get(arg1).matches("arg_1_\\d")
            assert stepMap.get(arg2).matches("arg_2_\\d")

            return CompletableFuture.completedFuture(DataFetcherResult.newResult().data("Huzzah!").build())
        }

        GraphQLObjectType containerObject = graphQLSchema.getObjectType("Container")

        final OperationDefinition queryOperation = TestHelper.query(query)
        final Field container = queryOperation.getSelectionSet().getSelectionsOfType(Field.class).get(0)
        final Field testField = container.getSelectionSet().getSelectionsOfType(Field.class).get(0)

        final GraphQLObjectType queryType = graphQLSchema.getQueryType()

        Map<ResolverArgumentDirective, Object> arguments = new HashMap<>()

        final ResolverArgumentDirective arg1Directive = ResolverArgumentDirective.newBuilder()
                .field(testField.getName())
                .argumentName(arg1)
                .graphQLInputType(Scalars.GraphQLInt)
                .build()

        final ResolverArgumentDirective arg2Directive = ResolverArgumentDirective.newBuilder()
                .field(testField.getName())
                .argumentName(arg2)
                .graphQLInputType(Scalars.GraphQLInt)
                .build()

        arguments.put(arg1Directive, argValue)
        arguments.put(arg2Directive, argValue)

        final ExecutionStepInfo containerExecutionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(containerObject)
                .field(MergedField.newMergedField(container).build())
                .path(ResultPath.parse("/container"))
                .arguments(Collections.emptyMap())
                .fieldDefinition(queryType.getFieldDefinition(container.getName()))
                .fieldContainer(queryType)
                .build()

        final ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLInt)
                .field(MergedField.newMergedField(testField).build())
                .path(ResultPath.parse("/container/test_field"))
                .arguments(Collections.emptyMap())
                .fieldDefinition(queryType.getFieldDefinition(testField.getName()))
                .fieldContainer(containerObject)
                .parentInfo(containerExecutionStepInfo)
                .build()

        DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .mergedField(MergedField.newMergedField(testField).build())
                .executionStepInfo(executionStepInfo)
                .graphQLSchema(graphQLSchema)
                .variables(Collections.emptyMap())
                .operationDefinition(queryOperation)
                .dataLoaderRegistry(dataLoaderRegistry)
                .fieldDefinition(containerObject.getFieldDefinition(testField.getName()))
                .arguments(Collections.emptyMap())
                .build()

        when:
        final DataFetcherResult<Object> result = dataFetcherHelper.callBatchLoaderWithArguments(env, arguments).join()

        then:
        result.getData() == "Huzzah!"
    }

    private Map<String, String> asArgumentMap(List<Argument> arguments) {
        return arguments.stream()
                .inject([:]) {map, it -> map << [(it.getName()): ((VariableReference) it.getValue()).getName()]}
    }

}