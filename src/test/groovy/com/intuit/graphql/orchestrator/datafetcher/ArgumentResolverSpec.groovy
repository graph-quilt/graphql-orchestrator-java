package com.intuit.graphql.orchestrator.datafetcher

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDirective
import graphql.*
import graphql.language.OperationDefinition
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingEnvironmentImpl
import graphql.schema.GraphQLSchema
import org.dataloader.DataLoaderRegistry
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static com.intuit.graphql.orchestrator.GraphQLOrchestrator.DATA_LOADER_REGISTRY_CONTEXT_KEY
import static com.intuit.graphql.orchestrator.TestHelper.query
import static org.mockito.Mockito.mock

class ArgumentResolverSpec extends Specification {

    private ArgumentResolver argumentResolver

    public GraphQL mockedGraphQL

    def setup() {
        mockedGraphQL = Mock(GraphQL.class)

        argumentResolver = ArgumentResolver.newBuilder()
                .graphQLBuilder({ schema -> mockedGraphQL })
                .build()
    }

    def "resolves Arguments"() {
        given:
        ExecutionResult aResult = ExecutionResultImpl.newExecutionResult()
                .data(Collections.emptyMap()).build()

        mockedGraphQL.executeAsync(_ as ExecutionInput) >> CompletableFuture.completedFuture(aResult)

        OperationDefinition aQuery = query("{ a }")
        OperationDefinition bQuery = query("{ b }")

        Map<ResolverArgumentDirective, OperationDefinition> resolverQueries = new HashMap<>()

        resolverQueries.put(mock(ResolverArgumentDirective.class), aQuery)
        resolverQueries.put(mock(ResolverArgumentDirective.class), bQuery)

        GraphQLContext graphQLContext = GraphQLContext.newContext()
                .of(DATA_LOADER_REGISTRY_CONTEXT_KEY, Mock(DataLoaderRegistry.class)).build()

        DataFetchingEnvironment env = DataFetchingEnvironmentImpl
                .newDataFetchingEnvironment()
                .context(graphQLContext)
                .graphQLSchema(mock(GraphQLSchema.class))
                .build()

        when:
        final Map<ResolverArgumentDirective, CompletableFuture<ExecutionResult>> results =
                argumentResolver.resolveArguments(env, resolverQueries)

        then:
        results.size() == 2
    }
}
