package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective
import com.intuit.graphql.orchestrator.xtext.DataFetcherContext
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

import java.util.concurrent.CompletableFuture

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

    ResolverArgumentDataFetcher dataFetcher = ResolverArgumentDataFetcher.newBuilder()
        .queriesByResolverArgument(Collections.emptyMap())
        .namespace(namespace)
        .serviceType(ServiceProvider.ServiceType.GRAPHQL)
        .build()

    def setup() {
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

    def "test Builder"() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(Collections.emptyMap())
                .namespace("").build()

        then:
        noExceptionThrown()
    }

    def "test Builder Resolver Data Is Null"() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Namespace Is Null"() {
        when:
        ResolverArgumentDataFetcher.newBuilder()
                .namespace(null)

        then:
        thrown(NullPointerException)
    }

    def "single Argument"() {
        given:
        DataFetcherResult<Object> dataFetcherResult = DataFetcherResult.newResult()
                .data("test_response").build()
        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> mockArgumentResults = new HashMap<>()
        mockArgumentResults.put(debtArgumentResolver, CompletableFuture.completedFuture(debtExecutionResult))

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> mockArgumentResults

        mockHelper.callBatchLoaderWithArguments(_ as DataFetchingEnvironment, _ as Map) >> CompletableFuture.completedFuture(dataFetcherResult)

        Map<ResolverArgumentDirective, OperationDefinition> map = new HashMap<>()

        map.put(debtArgumentResolver, debtQuery)

        ResolverArgumentDataFetcher dataFetcher = ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(map)
                .namespace(namespace).build()

        dataFetcher.argumentResolver = mockArgumentResolver
        dataFetcher.helper = mockHelper

        DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .dataLoaderRegistry(dataLoaderRegistry)
                .build()

        when:
        final DataFetcherResult<Object> result = dataFetcher
                .get(dataFetchingEnvironment).join()

        then:
        result.getData() == "test_response"
    }

    def "multiple Arguments"() {
        given:
        DataFetcherResult<Object> dataFetcherResult = DataFetcherResult.newResult()
                .data("test_response").build()

        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> mockResolverData = new HashMap<>()
        mockResolverData.put(debtArgumentResolver, CompletableFuture.completedFuture(debtExecutionResult))
        mockResolverData.put(incomeArgumentResolver, CompletableFuture.completedFuture(incomeExecutionResult))

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> { arguments ->
            assert ((Map<?, ?>) arguments[1]).size() == 2
            return mockResolverData
        }

        mockHelper.callBatchLoaderWithArguments(_ as DataFetchingEnvironment, _ as Map) >> CompletableFuture.completedFuture(dataFetcherResult)

        Map<ResolverArgumentDirective, OperationDefinition> map = new HashMap<>()
        map.put(debtArgumentResolver, debtQuery)
        map.put(incomeArgumentResolver, incomeQuery)

        dataFetcher = ResolverArgumentDataFetcher.newBuilder()
                .queriesByResolverArgument(map)
                .namespace(namespace).build()

        dataFetcher.argumentResolver = mockArgumentResolver
        dataFetcher.helper = mockHelper

        DataFetchingEnvironment dataFetchingEnvironment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .dataLoaderRegistry(dataLoaderRegistry)
                .build()

        when:
        DataFetcherResult<Object> result = dataFetcher.get(dataFetchingEnvironment).join()

        then:
        result.getData() == "test_response"
    }

    def "errors"() {
        given:
        ExecutionResult errorResult = ExecutionResultImpl.newExecutionResult()
                .addError(GraphqlErrorBuilder.newError().message("boom").build())
                .build()

        Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> resolvedArguments = new HashMap<>()
        resolvedArguments.put(debtArgumentResolver, CompletableFuture.completedFuture(errorResult))

        mockArgumentResolver.resolveArguments(_ as DataFetchingEnvironment, _ as Map) >> resolvedArguments

        dataFetcher.argumentResolver = mockArgumentResolver

        when:
        final DataFetcherResult<Object> result = dataFetcher
                .get(Mock(DataFetchingEnvironment.class)).join()

        then:
        result.getErrors().size() == 1
    }

    def "returns correct namespace"() {
        when:
        String actualNamespace = dataFetcher.getNamespace()

        then:
        actualNamespace == namespace
    }

    def "returns correct DataFetcherType"() {
        when:
        DataFetcherContext.DataFetcherType actualDataFetcherType = dataFetcher.getDataFetcherType()

        then:
        actualDataFetcherType == DataFetcherContext.DataFetcherType.RESOLVER_ARGUMENT
    }
}
