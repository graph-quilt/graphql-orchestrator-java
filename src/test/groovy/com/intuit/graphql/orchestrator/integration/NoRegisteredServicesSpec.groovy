package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import com.intuit.graphql.orchestrator.GraphQLOrchestrator.Builder
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionIdProvider
import graphql.schema.GraphQLFieldsContainer
import helpers.BaseIntegrationTestSpecification

class NoRegisteredServicesSpec extends BaseIntegrationTestSpecification {

    def "test Builder Without Service Null Execution Strategy"() {
        given:
        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        baseBuilder.queryExecutionStrategy(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Without Service Null Execution Id Provider"() {
        given:
        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        baseBuilder.executionIdProvider(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Without Service Null Instrumentations"() {
        given:
        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        baseBuilder.instrumentations(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Without Service Null Instrumentation at a given index"() {
        given:
        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        baseBuilder.instrumentation(0, null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Without Service Null Instrumentation"() {
        given:
        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        baseBuilder.instrumentation(null)

        then:
        thrown(NullPointerException)
    }

    def "test Builder Without Service"() {
        given:
        final RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder()
                .services(Collections.emptyList()).build().stitchGraph()

        final Builder baseBuilder = GraphQLOrchestrator.newOrchestrator()

        when:
        final GraphQLOrchestrator orchestrator = baseBuilder
                .runtimeGraph(runtimeGraph)
                .instrumentations(Collections.emptyList())
                .executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER)
                .queryExecutionStrategy(new AsyncExecutionStrategy())
                .mutationExecutionStrategy(new AsyncExecutionStrategy())
                .build()

        then:
        orchestrator != null
        ((GraphQLFieldsContainer) runtimeGraph.getExecutableSchema().getType("Query"))
                .getFieldDefinition("_namespace") != null
    }

}
