package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
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
    ServiceProvider enumProvider1, enumProvider2, invalidValueTypeProvider

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
        invalidValueTypeProvider = TestServiceProvider.newBuilder()
                .namespace("MANUFACTUER2")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(TestHelper.getFileMapFromList("top_level/federation/automakerInvalidInterface.graphqls"))
                .build()
    }

    def "Federation value types conflict if it contains unique fields"(){
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
        specUnderTest = createGraphQLOrchestrator([valueProvider1, valueProvider2])

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        queryType.getFieldDefinition("getProvider1Val").type.name == "sharedValueType"
        queryType.getFieldDefinition("getProvider2Val").type.name == "sharedValueType"
        //todo verify the new field in federation spec
    }

    def "Federation value types don't conflict if unique fields are inaccessible"(){
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
                    directive @inaccessible on OBJECT | INTERFACE | FIELD_DEFINITION
                    
                    type Query {
                        getProvider2Val: sharedValueType
                    }
                    type sharedValueType {
                        id: ID!
                        name: String @inaccessible
                        test: String @inaccessible
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
        specUnderTest = createGraphQLOrchestrator([valueProvider1, valueProvider2])

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        queryType.getFieldDefinition("getProvider1Val").type.name == "sharedValueType"
        queryType.getFieldDefinition("getProvider1Val").type.fieldDefinitionsByName.get("id") != null
        queryType.getFieldDefinition("getProvider1Val").type.fieldDefinitionsByName.get("name") == null
        queryType.getFieldDefinition("getProvider1Val").type.fieldDefinitionsByName.get("test") == null
        queryType.getFieldDefinition("getProvider2Val").type.name == "sharedValueType"
        queryType.getFieldDefinition("getProvider2Val").type.fieldDefinitionsByName.get("id") != null
        queryType.getFieldDefinition("getProvider2Val").type.fieldDefinitionsByName.get("name") == null
        queryType.getFieldDefinition("getProvider2Val").type.fieldDefinitionsByName.get("test") == null
    }

    def "Federation inaccessible value types are removed"(){
        given:
        def schema1 = """
                    type Query {
                        getProvider1Val: sharedValueType
                        getProvider1Val2: uniqueValueType
                    }
                    type sharedValueType {
                        id: ID!
                        name: String
                    }
                    type uniqueValueType {
                        id: ID!
                        name: String
                    }
                """

        def schema2 = """
                    directive @inaccessible on OBJECT | INTERFACE | FIELD_DEFINITION
                    type sharedValueType @inaccessible {
                        id: ID!
                        name: String
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
        specUnderTest = createGraphQLOrchestrator([valueProvider1, valueProvider2])

        then:
        final GraphQLObjectType queryType = specUnderTest?.runtimeGraph?.getOperation(Operation.QUERY)
        queryType.getFieldDefinition("getProvider1Val") == null
        queryType.getFieldDefinition("getProvider2Val") == null
        queryType.getFieldDefinition("getProvider1Val2").type.name == "uniqueValueType"
    }

    def "Federation Interface/enums value types can have unique fields"() {
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

    def "Federation Interface value types can have unique field error if other providers does not implement"() {
        given:
        String errorMsg

        when:
        try {
            createGraphQLOrchestrator([enumProvider1, invalidValueTypeProvider])
        } catch(RuntimeException ex) {
            errorMsg = ex.getMessage();
        }

        then:
        errorMsg != null
        errorMsg == "Implementing types Car do not contain the new field newField from interface Vehicle"
    }

    //TODO implementation of this portion will be later
    def "Federation Enum values unique fields cannot be called to providers that don't support"(){}
}
