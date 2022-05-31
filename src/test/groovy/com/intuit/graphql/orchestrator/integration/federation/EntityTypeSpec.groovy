package com.intuit.graphql.orchestrator.integration.federation


import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class EntityTypeSpec extends BaseIntegrationTestSpecification {
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

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 2
        queryType?.getFieldDefinition("employeeById") != null
    }

    def "Federation employee and inventory is stitched"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)

        graphQLSchema?.getQueryType()?.getFieldDefinitions()?.size() == 4
        queryType.getFieldDefinition("employeeById") != null
        queryType.getFieldDefinition("getSoldProducts") != null
        queryType.getFieldDefinition("getStoreByIdAndName") != null
    }

    def "Federation entities can be extended with extends directive and extend keyword"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider, reviewsProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        graphQLSchema.getType("Store")?.getFieldDefinition("review") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("review") != null
    }

    def "Federation multiple providers can extend entities"() {
        when:
        specUnderTest = createGraphQLOrchestrator([employeeProvider, inventoryProvider, reviewsProvider])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()

        graphQLSchema.getType("Employee")?.getFieldDefinition("id") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("username") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("password") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("review") != null
        graphQLSchema.getType("Employee")?.getFieldDefinition("favoriteItem") != null
    }
}
