package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionIdProvider
import graphql.schema.GraphQLObjectType
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import java.lang.instrument.Instrumentation

class ResolverOverrideSpec extends BaseIntegrationTestSpecification {

    ServiceProvider fooProvider, barProvider

    String barSchema = """
        type Query {
          getBar(barId: String): Bar
        }
        
        type Bar @key(fields: "id") {
            id: String
            barName: String
        }
        
        type Foo @extends @key(fields: "id") {
            id: String @external
            bar: Bar
        }
    """

    String fooSchema = """
        type Query {
          getFoo: Foo
        }
        
        type Foo @key(fields: "id"){
            id: String
            fooName: String
        }
    """

    String resolverSchema = """
        extend type Foo {
            bar : Bar @resolver(field: "getBar" arguments: [{name : "barId", value: "\$id"}])
        }

        type Bar{}

        directive @resolver(field: String, arguments: [ResolverArgument!]) on FIELD_DEFINITION

        input ResolverArgument {
            name : String!
            value : String!
        }
    """

    @Subject
    def specUnderTest

    void setup() {
        barProvider = TestServiceProvider.newBuilder()
                .namespace("BAR")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles( ImmutableMap.of("barSchema.graphqls", barSchema))
                .build()

        fooProvider = TestServiceProvider.newBuilder()
                .namespace("FOO")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles( ImmutableMap.of("fooSchema.graphqls", fooSchema, "resolver.graphqls", resolverSchema))
                .build()

    }

    def "Schema can successfully be stitched with resolver provider and entity extension"() {
        when:
            specUnderTest = createGraphQLOrchestrator([barProvider, fooProvider])
        then:
            specUnderTest != null
        GraphQLObjectType fooType = specUnderTest.getSchema().getType("Foo")
        fooType.fieldDefinitionsByName.size() == 3
        fooType.getFieldDefinition("id") != null
        fooType.getFieldDefinition("fooName") != null
        fooType.getFieldDefinition("bar") != null
    }

    def "Schema can successfully be stitched with resolver provider using domain type and entity extension"() {
        when:
        fooProvider.domainTypes().add("ResolverArgument")
        specUnderTest = createGraphQLOrchestrator([barProvider, fooProvider])
        then:
        specUnderTest != null
        GraphQLObjectType fooType = specUnderTest.getSchema().getType("Foo")
        fooType.fieldDefinitionsByName.size() == 3
        fooType.getFieldDefinition("id") != null
        fooType.getFieldDefinition("fooName") != null
        fooType.getFieldDefinition("bar") != null
    }

    GraphQLOrchestrator createGraphQLOrchestrator(List<ServiceProvider> services) {
        RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().services(services)
                .build().stitchGraph()

        GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator()
        graphql.execution.instrumentation.Instrumentation instrumentationMock = Mock(graphql.execution.instrumentation.Instrumentation.class)
        builder.runtimeGraph(runtimeGraph)
        builder.instrumentations(Collections.emptyList())
        builder.instrumentation(instrumentationMock)
        builder.instrumentation(0, instrumentationMock)
        builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
        builder.queryExecutionStrategy(new AsyncExecutionStrategy())
        builder.mutationExecutionStrategy(new AsyncExecutionStrategy())
        return builder.build()
    }
}
