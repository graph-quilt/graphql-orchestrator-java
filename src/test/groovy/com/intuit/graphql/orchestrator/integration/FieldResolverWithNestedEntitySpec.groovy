package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class FieldResolverWithNestedEntitySpec extends BaseIntegrationTestSpecification {

    def schemaA = """   
        type Query {
            aRootField(p1: String!): ARootType
        }

        type ARootType @key(fields: "aObjectField") {
            aObjectField: String!
        }   
    """

    def DOWNSTREAM_RESOLVER_QUERY = """query Resolver_Directive_Query {aRootField_0:aRootField(p1:"bObjectFieldValue1") {aObjectField}}"""
    def mockServiceResponseA = [
        (DOWNSTREAM_RESOLVER_QUERY) : [
            data: [
                aRootField_0 : [
                        aObjectField: "aObjectFieldValue1"
                ]
            ]
        ]
    ]

    def schemaB = """
        type Query {
            bTopField: [BObjectType]
        }
        
        type BObjectType {
            bObjectField: String
            aObject: ARootType @resolver(field: "aRootField" arguments: [{name : "p1", value: "\$bObjectField"}])             
        }

        type ARootType
        
        directive @resolver(field: String!, arguments: [B_ResolverArgument!]) on FIELD_DEFINITION
        input B_ResolverArgument { 
            name : String!
            value : String!
        }       
    """

    def DOWNSTREAM_QUERY= "query QUERY {bTopField {bObjectField}}"
    def mockServiceResponseB = [
        (DOWNSTREAM_QUERY): [
            data: [
                bTopField: [[ bObjectField: "bObjectFieldValue1"]]
            ]
        ]
    ]

    def schemaC = """
        type ARootType @extends @key(fields: "aObjectField") {
            aObjectField: String! @external
            cObjectField: String
        }
    """
    def DOWNSTREAM_ENTITY_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on ARootType {cObjectField}}}"
    def mockServiceResponseC = [
        (DOWNSTREAM_ENTITY_QUERY): [
            data: [
                _entities: [
                    [ __typename: "ARootType", cObjectField: "cObjectFieldValue" ]
                ]
            ]
        ]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        def serviceA = createQueryMatchingService("ServiceA", ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, schemaA, mockServiceResponseA)
        def serviceB = createQueryMatchingService("ServiceB", schemaB, mockServiceResponseB)
        def serviceC = createQueryMatchingService("ServiceC", ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, schemaC, mockServiceResponseC)
        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB, serviceC])
        graphQLSchema = specUnderTest.getSchema()
    }

    def "Nested FieldResolver"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aObject {
                        aObjectField  
                        cObjectField                                           
                    }
                }
            }
        """
        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        executionResult.data != null
        executionResult.data.bTopField[0] != null
        executionResult.data.bTopField[0].bObjectField == "bObjectFieldValue1"
        executionResult.data.bTopField[0].aObject != null
        executionResult.data.bTopField[0].aObject.aObjectField == "aObjectFieldValue1"
        executionResult.data.bTopField[0].aObject.cObjectField == "cObjectFieldValue"
    }

}