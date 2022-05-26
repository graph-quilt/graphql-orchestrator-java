package com.intuit.graphql.orchestrator.integration.federation

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class ValueTypeSpec extends BaseIntegrationTestSpecification {
    ServiceProvider enumProvider1, enumProvider2

    @Subject
    def specUnderTest

    void setup() {
        enumProvider1 = TestServiceProvider.newBuilder()
                .namespace("MANUFACTUER1")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/automakerEnum1.graphqls"))
                .build()
        enumProvider2 = TestServiceProvider.newBuilder()
                .namespace("MANUFACTUER2")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/automakerEnum2.graphqls"))
                .build()
    }

    def "Federation employee subgraph is stitched and value types are merged"() {
        given:

        when:
        specUnderTest = createGraphQLOrchestrator([enumProvider1, enumProvider2])

        then:
        final GraphQLSchema graphQLSchema = specUnderTest?.runtimeGraph?.getExecutableSchema()
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)

        queryType.getFieldDefinition("getMakers") != null
        queryType.getFieldDefinition("getAutos") != null
        queryType.getFieldDefinition("getManufacturers") != null
        queryType.getFieldDefinition("getCars") != null


        //validating enum types
        GraphQLEnumType enumType = graphQLSchema.getType("Manufacturer")
        enumType != null
        enumType.values.size() == 5
        enumType.getValue("TOYOTA") != null
        enumType.getValue("HONDA") != null
        enumType.getValue("CHEVROLET") != null
        enumType.getValue("FORD") != null
        enumType.getValue("VOLVO") != null

        //validating interface type
        GraphQLInterfaceType interfaceType = graphQLSchema.getType("Vehicle")
        interfaceType != null
        interfaceType.fieldDefinitions.size() == 3
        interfaceType.getFieldDefinition("wheels") != null
        interfaceType.getFieldDefinition("type") != null
        interfaceType.getFieldDefinition("features") != null

        GraphQLObjectType objectType = graphQLSchema.getType("Car")
        objectType != null
        objectType.fieldDefinitions.size() == 6
        objectType.getFieldDefinition("wheels") != null
        objectType.getFieldDefinition("type") != null
        objectType.getFieldDefinition("make") != null
        objectType.getFieldDefinition("model") != null
        objectType.getFieldDefinition("color") != null
        objectType.getFieldDefinition("features") != null
    }

}
