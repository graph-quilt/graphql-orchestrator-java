package helpers

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionIdProvider
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class BaseIntegrationTestSpecification extends Specification {

    def createMockGraphQLServiceProvider(String namespace, String sdl, String response) {
        createMockServiceProvider(namespace, ServiceProvider.ServiceType.GRAPHQL, sdl, response)
    }

    def createMockRESTServiceProvider(String namespace, String sdl, String response) {
        createMockServiceProvider(namespace, ServiceProvider.ServiceType.REST, sdl, response)
    }

    private def createMockServiceProvider(String namespace, ServiceProvider.ServiceType serviceType, String sdl, String response) {
        ServiceProvider mockServiceProvider = Stub(ServiceProvider.class)

        mockServiceProvider.nameSpace >> namespace
        mockServiceProvider.sdlFiles() >> ['"schema.graphqls"': sdl]
        mockServiceProvider.seviceType() >> serviceType
        mockServiceProvider.query(_ as ExecutionInput, _ as GraphQLContext) >> CompletableFuture.completedFuture(new JsonSlurper().parseText(response))

        mockServiceProvider
    }

    def createGraphQLOrchestrator(List<ServiceProvider> services) {

        SchemaStitcher schemaStitcher = SchemaStitcher.newBuilder()
                .services(services)
                .build()

        return GraphQLOrchestrator.newOrchestrator()
                .runtimeGraph(schemaStitcher.stitchGraph())
                .instrumentations(Collections.emptyList())
                .executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER)
                .queryExecutionStrategy(new AsyncExecutionStrategy())
                .mutationExecutionStrategy(new AsyncExecutionStrategy())
                .build()
    }

}
