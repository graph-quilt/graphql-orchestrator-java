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
            arootField: ARootType
        }

        type ARootType {
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

    def QUERY_A = "query Resolver_Directive_Query {arootField {aTopField_0:aTopField(p1:\"bObjectFieldValue1\") {aObjectField}} arootField {aTopField_1:aTopField(p1:\"bObjectFieldValue2\") {aObjectField}}}"
    def mockServiceResponseA = [
            (QUERY_A): [data: [
                    arootField : [
                            aTopField_0: [ aObjectField: "aObjectFieldValue1"],
                            aTopField_1: [ aObjectField: "aObjectFieldValue2"]
                    ]
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

    def schemaC = """
        type Query {
            cTopField: CObjectType!
        }

        type CObjectType {
            cObjectField: String
        }
    """

    def QUERY_C = "query Resolver_Directive_Query {cTopField_0:cTopField {cObjectField} cTopField_1:cTopField {cObjectField}}"
    def mockServiceResponseC = [
            (QUERY_C): [data: [
                    cTopField_0: [ cObjectField: "cObjectFieldValue1"],
                    cTopField_1: [ cObjectField: "cObjectFieldValue2"],
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
        specUnderTest.runtimeGraph.codeRegistry.dataFetcherMap.size() == 6
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "BObjectType.bObjectField")
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "Query.cTopField")
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "Query._namespace")
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "Query.arootField")
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "Query.bTopField")
        checkIfKeyExistsInDataFetcherMap(specUnderTest, "AObjectType.cTopField")

        executionResult.getErrors().isEmpty()
        executionResult?.data?.bTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField == "aObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.cTopField?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.cTopField?.cObjectField == "cObjectFieldValue1"
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField == "aObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.cTopField?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.cTopField?.cObjectField == "cObjectFieldValue2"
    }

}