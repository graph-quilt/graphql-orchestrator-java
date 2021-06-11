package com.intuit.graphql.orchestrator.datafetcher;

import static com.intuit.graphql.orchestrator.TestHelper.query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective;
import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.VariableReference;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ResolverArgumentDataFetcherHelperTest {

  String schema = "schema { query: QueryType } type QueryType { test_field: Int container: Container } type Container { test_field: Int }";

  @Mock
  public DataLoader<DataFetchingEnvironment, DataFetcherResult<Object>> mockDataLoader;

  private GraphQLSchema graphQLSchema;

  private ResolverArgumentDataFetcherHelper dataFetcherHelper;

  private static final String namespace = "test_namespace";

  private DataLoaderRegistry dataLoaderRegistry;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    graphQLSchema = TestHelper.schema(schema);
    dataFetcherHelper = new ResolverArgumentDataFetcherHelper(namespace);
    dataLoaderRegistry = new DataLoaderRegistry();
    dataLoaderRegistry.register(namespace, mockDataLoader);
  }

  @Test
  public void singleArgumentSuccess() {
    ArgumentCaptor<DataFetchingEnvironment> envCaptor = ArgumentCaptor.forClass(DataFetchingEnvironment.class);
    when(mockDataLoader.load(envCaptor.capture()))
        .thenReturn(CompletableFuture.completedFuture(DataFetcherResult.newResult().data("Huzzah!").build()));

    String query = "{ test_field }";
    String argumentName = "arg";
    String expectedArgumentReferenceName = "arg_0";

    final OperationDefinition queryOperation = query(query);
    final Field testFieldNameField = (Field) queryOperation.getSelectionSet().getSelections().get(0);
    final GraphQLObjectType queryType = graphQLSchema.getQueryType();

    Map<ResolverArgumentDirective, Object> arguments = new HashMap<>();

    final ResolverArgumentDirective resolverArgumentDirective = ResolverArgumentDirective.newBuilder()
        .field(testFieldNameField.getName())
        .argumentName(argumentName)
        .graphQLInputType(GraphQLInputObjectType.newInputObject().name("Test_Input_Object_Type").build())
        .build();

    arguments.put(resolverArgumentDirective, "test_value");

    GraphQLOutputType type = Scalars.GraphQLInt;

    final ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
        .type(type)
        .field(MergedField.newMergedField(testFieldNameField).build())
        .path(ExecutionPath.parse("/test_field"))
        .arguments(Collections.emptyMap())
        .fieldDefinition(queryType.getFieldDefinition(testFieldNameField.getName()))
        .fieldContainer(queryType)
        .build();

    DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(MergedField.newMergedField(testFieldNameField).build())
        .executionStepInfo(executionStepInfo)
        .graphQLSchema(graphQLSchema)
        .variables(Collections.emptyMap())
        .operationDefinition(queryOperation)
        .dataLoaderRegistry(dataLoaderRegistry)
        .fieldDefinition(queryType.getFieldDefinition(testFieldNameField.getName()))
        .arguments(Collections.emptyMap())
        .build();

    final DataFetcherResult<Object> result = dataFetcherHelper.callBatchLoaderWithArguments(env, arguments).join();

    DataFetchingEnvironment modifiedEnvironment = envCaptor.getValue();

    assertThat(result.getData()).isEqualTo("Huzzah!");

    assertThat((String) modifiedEnvironment.getArgument(argumentName))
        .isEqualTo("test_value");

    assertThat(modifiedEnvironment.getMergedField().getSingleField().getArguments().get(0))
        .matches(argument -> argument.getName().equals(argumentName))
        .matches(argument -> ((VariableReference) argument.getValue()).getName().equals(expectedArgumentReferenceName));

    assertThat(modifiedEnvironment.getExecutionStepInfo().getField().getSingleField().getArguments().get(0))
        .matches(argument -> argument.getName().equals(argumentName))
        .matches(argument -> ((VariableReference) argument.getValue()).getName().equals(expectedArgumentReferenceName));

    assertThat(modifiedEnvironment.getExecutionStepInfo().<VariableReference>getArgument(argumentName))
        .matches(reference -> reference.getName().equals(expectedArgumentReferenceName));
  }

  @Test
  public void multipleArguments() {
    ArgumentCaptor<DataFetchingEnvironment> envCaptor = ArgumentCaptor.forClass(DataFetchingEnvironment.class);
    when(mockDataLoader.load(envCaptor.capture()))
        .thenReturn(CompletableFuture.completedFuture(DataFetcherResult.newResult().data("Huzzah!").build()));

    String query = "{ container { test_field } }";
    String arg1 = "arg_1";
    String arg2 = "arg_2";

    String argValue = "test_value";

    GraphQLObjectType containerObject = graphQLSchema.getObjectType("Container");

    final OperationDefinition queryOperation = query(query);
    final Field container = queryOperation.getSelectionSet().getSelectionsOfType(Field.class).get(0);
    final Field testField = container.getSelectionSet().getSelectionsOfType(Field.class).get(0);

    final GraphQLObjectType queryType = graphQLSchema.getQueryType();

    Map<ResolverArgumentDirective, Object> arguments = new HashMap<>();

    final ResolverArgumentDirective arg1Directive = ResolverArgumentDirective.newBuilder()
        .field(testField.getName())
        .argumentName(arg1)
        .graphQLInputType(Scalars.GraphQLInt)
        .build();

    final ResolverArgumentDirective arg2Directive = ResolverArgumentDirective.newBuilder()
        .field(testField.getName())
        .argumentName(arg2)
        .graphQLInputType(Scalars.GraphQLInt)
        .build();

    arguments.put(arg1Directive, argValue);
    arguments.put(arg2Directive, argValue);

    final ExecutionStepInfo containerExecutionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
        .type(containerObject)
        .field(MergedField.newMergedField(container).build())
        .path(ExecutionPath.parse("/container"))
        .arguments(Collections.emptyMap())
        .fieldDefinition(queryType.getFieldDefinition(container.getName()))
        .fieldContainer(queryType)
        .build();

    final ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
        .type(Scalars.GraphQLInt)
        .field(MergedField.newMergedField(testField).build())
        .path(ExecutionPath.parse("/container/test_field"))
        .arguments(Collections.emptyMap())
        .fieldDefinition(queryType.getFieldDefinition(testField.getName()))
        .fieldContainer(containerObject)
        .parentInfo(containerExecutionStepInfo)
        .build();

    DataFetchingEnvironment env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
        .mergedField(MergedField.newMergedField(testField).build())
        .executionStepInfo(executionStepInfo)
        .graphQLSchema(graphQLSchema)
        .variables(Collections.emptyMap())
        .operationDefinition(queryOperation)
        .dataLoaderRegistry(dataLoaderRegistry)
        .fieldDefinition(containerObject.getFieldDefinition(testField.getName()))
        .arguments(Collections.emptyMap())
        .build();

    final DataFetcherResult<Object> result = dataFetcherHelper.callBatchLoaderWithArguments(env, arguments).join();

    DataFetchingEnvironment modifiedEnvironment = envCaptor.getValue();

    assertThat(result.getData()).isEqualTo("Huzzah!");

    assertThat((String) modifiedEnvironment.getArgument(arg1))
        .isEqualTo("test_value");

    assertThat(asArgumentMap(modifiedEnvironment.getMergedField().getSingleField().getArguments()))
        .hasSize(2)
        .containsKeys(arg1, arg2)
        .matches(map -> map.get(arg1).matches("arg_1_\\d"))
        .matches(map -> map.get(arg2).matches("arg_2_\\d"));

    assertThat(asArgumentMap(modifiedEnvironment.getExecutionStepInfo().getField().getSingleField().getArguments()))
        .hasSize(2)
        .containsOnlyKeys(arg1, arg2)
        .matches(map -> map.get(arg1).matches("arg_1_\\d"))
        .matches(map -> map.get(arg2).matches("arg_2_\\d"));
  }

  private Map<String, String> asArgumentMap(List<Argument> arguments) {
    return arguments.stream()
        .collect(Collectors.toMap(Argument::getName, argument -> ((VariableReference) argument.getValue()).getName()));
  }

}