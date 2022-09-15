package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class EntityWithNestedFieldResolverSpec extends BaseIntegrationTestSpecification {

    def schemaA = """   
        type Query {
            arootField: ARootType
        }

        type ARootType {
            aTopField(p1: String!): AObjectType!
        }
        
        type BObjectType @key(fields: "bObjectField") @extends {
            bObjectField: String @external 
            aTopField: AObjectType          
        }        
        
        type AObjectType {
            aObjectField: String
            aObjectField2: NestedType
        }
        
        type NestedType {
            typeId: String
            cTopFieldLink: CObjectType @resolver(field: "cTopField")
            dTopFieldLink: DObjectType @resolver(field: "dTopField", arguments: [{name : "p1", value: "\$typeId"}])
        }
        
        type CObjectType
        
        type DObjectType
        
        directive @resolver(field: String!, arguments: [A_ResolverArgument!]) on FIELD_DEFINITION
        input A_ResolverArgument { # If this is type, dapi does not throws error
            name : String!
            value : String!
        }         
    """

    def QUERY_A = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on BObjectType {aTopField {aObjectField aObjectField2 {typeId} __typename}}}}"
    def mockServiceResponseA = [
            (QUERY_A): [data: [
                    _entities : [
                            [aTopField: [ aObjectField: "aObjectFieldValue1", aObjectField2: [typeId: "type1"], __typename: "AObjectType"]],
                            [aTopField: [ aObjectField: "aObjectFieldValue2", aObjectField2: [typeId: "type2"], __typename: "AObjectType"]]
                    ]
            ]]
    ]

    def schemaB = """
        type Query {
            bTopField: [BObjectType]
        }
        
        type BObjectType @key(fields: "bObjectField") {
            bObjectField: String            
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

    def schemaD = """
        type Query {
            dTopField(p1: String): DObjectType!
        }

        type DObjectType {
            dObjectField: String
        }
    """

    def QUERY_D = "query Resolver_Directive_Query {dTopField_0:dTopField(p1:\"type1\") {dObjectField} dTopField_1:dTopField(p1:\"type2\") {dObjectField}}"
    def mockServiceResponseD = [
            (QUERY_D): [data: [
                    dTopField_0: [ dObjectField: "dObjectFieldValue1"],
                    dTopField_1: [ dObjectField: "dObjectFieldValue2"],
            ]]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        def serviceA = createQueryMatchingService("ServiceA", ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, schemaA, mockServiceResponseA)
        def serviceB = createQueryMatchingService("ServiceB", ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, schemaB, mockServiceResponseB)
        def serviceC = createQueryMatchingService("ServiceC", schemaC, mockServiceResponseC)
        def serviceD = createQueryMatchingService("ServiceD", schemaD, mockServiceResponseD)
        specUnderTest = createGraphQLOrchestrator([serviceA, serviceB, serviceC, serviceD])
        graphQLSchema = specUnderTest.getSchema()
    }

    def "Query Single Nested FieldResolver"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField
                        aObjectField2 {
                            typeId
                            cTopFieldLink {
                                cObjectField
                            }
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
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField == "aObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.typeId == "type1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.cTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.cTopFieldLink?.cObjectField == "cObjectFieldValue1"
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField == "aObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.typeId == "type2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.cTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.cTopFieldLink?.cObjectField == "cObjectFieldValue2"
    }

    def "Query Single Nested FieldResolver with Required Field not selected"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField
                        aObjectField2 {
                            dTopFieldLink {
                                dObjectField
                            }
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
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField == "aObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue1"
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField == "aObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue2"
    }

     def "Query Single Nested FieldResolver with Required Field selected"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField
                        aObjectField2 {
                            typeId
                            dTopFieldLink {
                                dObjectField
                            }
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
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField == "aObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.typeId == "type1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue1"
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField == "aObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.typeId == "type2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue2"
    }

    def "Query Multiple Nested FieldResolver"() {
        given:
        def graphqlQuery =    """
            {
                bTopField {
                    bObjectField
                    aTopField {
                        aObjectField
                        aObjectField2 {
                            cTopFieldLink {
                                cObjectField
                            }
                            dTopFieldLink {
                                dObjectField
                            }
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
        executionResult?.data?.bTopField[0]?.size() == 2
        executionResult?.data?.bTopField[1]?.size() == 2
        executionResult?.data?.bTopField[0]?.bObjectField == "bObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField == "aObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.typeId == null // not selected in query
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.cTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.cTopFieldLink?.cObjectField == "cObjectFieldValue1"
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[0]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue1"
        executionResult?.data?.bTopField[1]?.bObjectField == "bObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.size() == 2
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField == "aObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.typeId == null // not selected in query
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.cTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.cTopFieldLink?.cObjectField == "cObjectFieldValue2"
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.size() == 1
        executionResult?.data?.bTopField[1]?.aTopField?.aObjectField2?.dTopFieldLink?.dObjectField == "dObjectFieldValue2"
    }

}