package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.schema.*
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class RepeatableDirectiveSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        type Query {
            pet: Pet
        }               

        interface Pet @petDirective(arg: "one") @petDirective(arg: "two") {
          id: ID! @fieldDirective(arg: "A") @fieldDirective(arg: "B")
        }
        
        type Dog implements Pet @petDirective(arg: "one") @petDirective(arg: "two") {
            id: ID! @fieldDirective(arg: "A") @fieldDirective(arg: "B")
        }
        
        extend interface Pet @petDirective(arg: "three")        
        extend type Dog @petDirective(arg: "three")
        
        scalar SomeNewScalar @scalarDirective(arg: "arg1") @scalarDirective(arg: "arg2")
        enum SomeNewEnum @enumDirective(arg: "1") @enumDirective(arg: "2") {
          NORTH
          EAST
          SOUTH
          WEST
        }
        
        union SomeNewUnion @unionDirective(arg: "1") @unionDirective(arg: "2") = Dog 
        
        directive @petDirective(arg: String!) repeatable on OBJECT | INTERFACE
        directive @fieldDirective(arg: String!) repeatable on FIELD_DEFINITION
        directive @scalarDirective(arg: String!) repeatable on SCALAR        
        directive @unionDirective(arg: String!) repeatable on UNION
        directive @enumDirective(arg: String!) repeatable on ENUM
        directive @inputObjectDirective(arg: String!) repeatable on INPUT_OBJECT                 
    """

    def mockServiceResponse = [
            data: [
                    book: [ id: "book-1"]
            ]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
        graphQLSchema = specUnderTest.getSchema()
    }

    def "can build schema with repeatable directives on interface type, object type and field definitions"() {
        given: "graphQLSchema"

        and:
        GraphQLInterfaceType graphQLInterfaceType = (GraphQLInterfaceType) graphQLSchema.getType("Pet")
        GraphQLObjectType graphQLObjectType = (GraphQLObjectType) graphQLSchema.getType("Dog")

        expect:
        graphQLInterfaceType.getDirectives("petDirective").size() == 3
        graphQLInterfaceType.getFieldDefinition("id").getDirectives("fieldDirective").size() == 2
        graphQLObjectType.getDirectives("petDirective").size() == 3
        graphQLObjectType.getFieldDefinition("id").getDirectives("fieldDirective").size() == 2
    }

    def "can build schema with repeatable directives on scalar type definitions"() {
        given: "graphQLSchema"

        and:
        GraphQLScalarType scalarType = (GraphQLScalarType) graphQLSchema.getType("SomeNewScalar")

        expect:
        scalarType.getDirectives("scalarDirective").size() == 2
    }

    def "can build schema with repeatable directives on enum type definitions"() {
        given: "graphQLSchema"

        and:
        GraphQLEnumType enumType = (GraphQLEnumType) graphQLSchema.getType("SomeNewEnum")

        expect:
        enumType.getDirectives("enumDirective").size() == 2
    }

    def "can build schema with repeatable directives on union type definitions"() {
        given: "graphQLSchema"

        and:
        GraphQLUnionType unionType = (GraphQLUnionType) graphQLSchema.getType("SomeNewUnion")

        expect:
        unionType.getDirectives("unionDirective").size() == 2
    }

}
