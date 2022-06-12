package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class FieldResolverReturnsNestedFieldWithNullSpec extends BaseIntegrationTestSpecification {

    def schemaA = """   
        type Query {
            arootField: ARootType
        }

        type ARootType {
            aTopField(p1: String!): AObjectType!
        }
        
        type AObjectType {
            aObjectField: String
        }           
    """

    def QUERY_A = "query Resolver_Directive_Query {arootField {aTopField_0:aTopField(p1:\"bObjectFieldValue1\") {aObjectField}} arootField {aTopField_1:aTopField(p1:\"bObjectFieldValue2\") {aObjectField}}}"
    def mockServiceResponseA = [
            (QUERY_A): [data: [
                    arootField : null
            ]]
    ]

    def schemaB = """
        type Query {
            bTopField: [BObjectType]
        }
        
        type BObjectType {
            bObjectField: String
            aTopField: AObjectType @resolver(field: "arootField.aTopField", arguments: [{name : "p1", value: "\$bObjectField"}])
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
                    bTopField: [[ bObjectField: "bObjectFieldValue1"], [ bObjectField: "bObjectFieldValue2"]]
            ]]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        def serviceA = createQueryMatchingService("ServiceA", schemaA, mockServiceResponseA)
        def serviceB = createQueryMatchingService("ServiceB", schemaB, mockServiceResponseB)
        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB])
        graphQLSchema = specUnderTest.getSchema()
    }

    def "resolver returns nested field with null"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField                                    
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
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField == null
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField == null
    }

}