package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective
import com.intuit.graphql.orchestrator.TestHelper
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphqlErrorBuilder
import graphql.Scalars
import graphql.execution.DataFetcherResult
import graphql.language.OperationDefinition
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import helpers.BaseIntegrationTestSpecification
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.junit.Test
import org.mockito.ArgumentCaptor

import java.util.Collections
import java.util.HashMap
import java.util.Map
import java.util.concurrent.CompletableFuture

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyMap
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.doReturn

class ResolverArgumentDataFetcherSpec extends BaseIntegrationTestSpecification {

    public String namespace = "TEST"

    public String debtFieldName = "debt"

    public String incomeFieldName = "income"

    public ResolverArgumentDirective debtArgumentResolver = ResolverArgumentDirective.newBuilder()
            .argumentName("test_argument_name")
            .field(debtFieldName)
            .graphQLInputType(Scalars.GraphQLInt)
            .build()

    public ExecutionResult debtExecutionResult = ExecutionResultImpl
            .newExecutionResult()
            .data(Collections.singletonMap(debtFieldName, 1_000))
            .build()

    ResolverArgumentDirective incomeArgumentResolver = ResolverArgumentDirective.newBuilder()
            .argumentName("second_argument_name")
            .field(debtFieldName)
            .graphQLInputType(Scalars.GraphQLInt)
            .build()

    public ExecutionResult incomeExecutionResult = ExecutionResultImpl
            .newExecutionResult()
            .data(Collections.singletonMap(debtFieldName, 1_000))
            .build()

    public OperationDefinition debtQuery = TestHelper.query("{" + debtFieldName + "}")
    public OperationDefinition incomeQuery = TestHelper.query("{" + incomeFieldName + "}")

    public ResolverArgumentDataFetcherHelper mockHelper

    public ArgumentResolver mockArgumentResolver

    public ExecutionResult executionResult

    private DataLoaderRegistry dataLoaderRegistry

    void setup() {
        mockHelper = Mock(ResolverArgumentDataFetcherHelper.class)
        mockArgumentResolver = Mock(ArgumentResolver.class)

        executionResult = ExecutionResultImpl.newExecutionResult()
                .data(new HashMap<String, Object>() {{
                    put(debtFieldName, 1_000_000)
                }})
                .build()

        dataLoaderRegistry = new DataLoaderRegistry()
        BatchLoader<Object, Object> batchLoader = { a -> null }
        dataLoaderRegistry.register(namespace, DataLoader.newDataLoader(batchLoader))
    }

    void testBuilder() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(Collections.emptyMap())
                .namespace("").build()

        then:
        noExceptionThrown()
    }

    void testBuilderResolverDataIsNull() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(null)

        then:
        thrown(NullPointerException)
    }

    void testBuilderNamespaceIsNull() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .namespace(null)

        then:
        thrown(NullPointerException)
    }

    void singleArgument() {
        given:
        DataFetcherResult<Object> dataFetcherResult = DataFetcherResult.newResult()
                .data("test_response").build()
        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> mockArgumentResults = new HashMap<>()
        mockArgumentResults.put(debtArgumentResolver, CompletableFuture.completedFuture(debtExecutionResult))

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> mockArgumentResults

        mockHelper.callBatchLoaderWithArguments(_ as DataFetchingEnvironment, _ as Map) >> CompletableFuture.completedFuture(dataFetcherResult)

        Map<ResolverArgumentDirective, OperationDefinition> map = new HashMap<>()

        map.put(debtArgumentResolver, debtQuery)

        ResolverArgumentDataFetcher resolverArgumentDataFetcher = ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(map)
                .namespace(namespace).build()

        resolverArgumentDataFetcher.argumentResolver = mockArgumentResolver
        resolverArgumentDataFetcher.helper = mockHelper

        DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .dataLoaderRegistry(dataLoaderRegistry)
                .build()

        when:
        final DataFetcherResult<Object> result = resolverArgumentDataFetcher
                .get(dataFetchingEnvironment).join()

        then:
        result.getData() == "test_response"
    }

    void multipleArguments() {
        given:
        DataFetcherResult<Object> dataFetcherResult = DataFetcherResult.newResult()
                .data("test_response").build()

        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> mockResolverData = new HashMap<>();
        mockResolverData.put(debtArgumentResolver, CompletableFuture.completedFuture(debtExecutionResult));
        mockResolverData.put(incomeArgumentResolver, CompletableFuture.completedFuture(incomeExecutionResult));

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> mockResolverData

        mockHelper.callBatchLoaderWithArguments(_ as DataFetchingEnvironment, _ as Map) >> CompletableFuture.completedFuture(dataFetcherResult)

        Map<ResolverArgumentDirective, OperationDefinition> map = new HashMap<>()
        map.put(debtArgumentResolver, debtQuery)
        map.put(incomeArgumentResolver, incomeQuery)

        ResolverArgumentDataFetcher dataFetcher = ResolverArgumentDataFetcher.newBuilder()
                .namespace(namespace)
                .queriesByResolverArgument(map)
                .build()

        dataFetcher.argumentResolver = mockArgumentResolver
        dataFetcher.helper = mockHelper

        DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .dataLoaderRegistry(dataLoaderRegistry)
                .build()

        when:
        //  PIC::TODO : this line is running into null pointer exception
        DataFetcherResult<Object> result = dataFetcher.get(dataFetchingEnvironment).join()

        then:
        result.getData() == "test_response"

        1 * mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> { arguments ->
            assert ((Map<?, ?>) arguments[1]).size() == 2
        }
    }

    void errors() {
        given:
        ExecutionResult errorResult = ExecutionResultImpl.newExecutionResult()
                .addError(GraphqlErrorBuilder.newError().message("boom").build())
                .build()

        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> resolvedArguments = new HashMap<>()
        resolvedArguments.put(debtArgumentResolver, CompletableFuture.completedFuture(errorResult))

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> resolvedArguments

        ResolverArgumentDataFetcher resolverArgumentDataFetcher = ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(Collections.emptyMap())
                .namespace(namespace)
                .build()

        resolverArgumentDataFetcher.argumentResolver = mockArgumentResolver

        when:
        final DataFetcherResult<Object> result = resolverArgumentDataFetcher
                .get(Mock(DataFetchingEnvironment.class)).join()

        then:
        result.getErrors().size() == 1
    }
}