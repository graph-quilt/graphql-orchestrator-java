package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import static org.assertj.core.api.Assertions.assertThat

class StitchingTopLevelSpec extends BaseIntegrationTestSpecification {
    ServiceProvider employeeProvider, inventoryProvider, reviewsProvider

    @Subject
    def specUnderTest

    void setup() {
        employeeProvider = TestServiceProvider.newBuilder()
                .namespace("Employee")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/employee.graphqls"))
                .build()

        inventoryProvider = TestServiceProvider.newBuilder()
                .namespace("Inventory")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/inventory.graphqls"))
                .build()

        reviewsProvider = TestServiceProvider.newBuilder()
                .namespace("Review")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/review.graphqls"))
                .build()    }

    def "Federation employee subgraph is stitched"() {
        given:

        when:
        specUnderTest = createGraphQLOrchestrator(employeeProvider)

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema();
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY);

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 1
        queryType?.getFieldDefinition("employeeById") != null
    }

    def "Federation employee and inventory is stitched"() {
        when:
        specUnderTest = createGraphQLOrchestrator(employeeProvider, inventoryProvider)

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 3
        queryType.getFieldDefinition("employeeById") != null
        queryType.getFieldDefinition("getSoldProducts") != null
        queryType.getFieldDefinition("getStoreByIdAndName") != null
    }

    def "Federation shared value types do not conflict"(){
        given:
        def schema1 = """
                    type Query {
                        getProvider1Val: sharedValueType
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                    }
                """

        def schema2 = """
                    type Query {
                        getProvider2Val: sharedValueType
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                        test: String
                    }
                """

        def valueProvider1 = TestServiceProvider.newBuilder()
                .namespace("A")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema1", schema1))
                .build()

        def valueProvider2 = TestServiceProvider.newBuilder()
                .namespace("B")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema2", schema2))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator(valueProvider1, valueProvider2)

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        queryType.getFieldDefinition("getProvider1Val").type.name == "sharedValueType"
        queryType.getFieldDefinition("getProvider2Val").type.name == "sharedValueType"
        //todo verify the new field in federation spec
    }

    def "Federation entities can be extended with extends directive and extend keyword"() {
        when:
        specUnderTest = createGraphQLOrchestrator(employeeProvider, inventoryProvider, reviewsProvider)

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        graphQLSchema.getType("Store")?.getFieldDefinition("review") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("review") != null
    }
}
