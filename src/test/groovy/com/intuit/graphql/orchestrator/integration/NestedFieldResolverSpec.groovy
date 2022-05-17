package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class NestedFieldResolverSpec extends BaseIntegrationTestSpecification {

    def schemaA = """   
        type Query {
            aTopField(p1: String!): AObjectType!
        }
        
        type AObjectType {
            aObjectField: String
            cTopField: CObjectType @resolver(field: "cTopField")
        }   
        
        type CObjectType
        
        directive @resolver(field: String!, arguments: [A_ResolverArgument!]) on FIELD_DEFINITION
        input A_ResolverArgument { # If this is type, dapi does not throws error
            name : String!
            value : String!
        }         
    """

    def QUERY_A = "query Resolver_Directive_Query {aTopField_0:aTopField(p1:\"bObjectFieldValue\") {aObjectField}}"
    def mockServiceResponseA = [
            (QUERY_A): [data: [
                    aTopField_0: [ aObjectField: "aObjectFieldValue"]
            ]]
    ]

    def schemaB = """
        type Query {
            bTopField: BObjectType
        }
        
        type BObjectType {
            bObjectField: String
            aTopField: AObjectType @resolver(field: "aTopField", arguments: [{name : "p1", value: "\$bObjectField"}])
        }

        type AObjectType
        
        directive @resolver(field: String!, arguments: [ResolverArgument!]) on FIELD_DEFINITION
        input ResolverArgument { # If this is type, dapi does not throws error
            name : String!
            value : String!
        }             
    """

    def QUERY_B = "query QUERY {bTopField {bObjectField}}"
    def mockServiceResponseB = [
            (QUERY_B) : [ data: [
                    bTopField: [ bObjectField: "bObjectFieldValue"]
            ]]
    ]

    def schemaC = """
        type Query {
            cTopField: CObjectType!
        }

        type CObjectType {
            cObjectField: String
        }
    """

    def QUERY_C = "query Resolver_Directive_Query {cTopField_0:cTopField {cObjectField}}"
    def mockServiceResponseC = [
            (QUERY_C): [data: [
                    cTopField_0: [ cObjectField: "cObjectFieldValue"]
            ]]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        def serviceA = createQueryMatchingService("ServiceA", schemaA, mockServiceResponseA)
        def serviceB = createQueryMatchingService("ServiceB", schemaB, mockServiceResponseB)
        def serviceC = createQueryMatchingService("ServiceC", schemaC, mockServiceResponseC)
        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB, serviceC])
        graphQLSchema = specUnderTest.getSchema()
    }

    def "Nested FieldResolver"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField  
                        cTopField {
                            cObjectField
                        }                                              
                    }
                }
            }
        """
        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        executionResult?.data?.bTopField?.size() == 2
        executionResult?.data?.bTopField?.bObjectField == "bObjectFieldValue"
        executionResult?.data?.bTopField?.aTopField?.size() == 2
        executionResult?.data?.bTopField?.aTopField?.aObjectField == "aObjectFieldValue"
        executionResult?.data?.bTopField?.aTopField?.cTopField?.size() == 1
        executionResult?.data?.bTopField?.aTopField?.cTopField?.cObjectField == "cObjectFieldValue"

    }

}