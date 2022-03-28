package com.intuit.graphql.orchestrator.integration


import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class InterfaceImplementInterfaceSpec extends BaseIntegrationTestSpecification {

    def graphqlQuery = """
        query {
            pets {
               edges {
                    ... on DogEdge {
                        node {
                            id name isServiceDog
                            __typename
                        }
                        __typename
                    }
               }
            }
        }
    """

    def testSchema = """
        type Query {
            pets: PetConnection
        }
        
        # Relay
        interface Node {
            id: ID!
        }
        
        interface Edge {
            cursor: String!
            node: Node
        }
        
        interface Connection {
            edges: [Edge]
        }
        
        # Pet
        interface Pet implements Node {
            id: ID!
            name: String!
        }
        
        interface PetEdge implements Edge {
            cursor: String!
            node: Pet
        }
        
        interface PetConnection implements Connection {
            edges: [PetEdge]
        }
        
        # DOG
        type Dog implements Pet & Node {
            id: ID!
            name: String!
            isServiceDog: Boolean
        }
        
        type DogEdge implements PetEdge & Edge {
            cursor: String!
            node: Dog
        }
        
        type DogConnection implements PetConnection & Connection {
            edges: [DogEdge]
        }
    """

    def mockServiceResponse = [
            data: [
                    pets: [
                            edges     : [[
                                             node:
                                             [
                                                     id          : "dog-1",
                                                     name        : "Lassie",
                                                     isServiceDog: false,
                                                     __typename  : "Dog"
                                             ] ,
                                             __typename: "DogEdge"
                                         ]
                            ],
                            __typename: "DogConnection"
                    ]
            ]
    ]

    @Subject
    def specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
    }

    def "interface can extends another interface"() {
        given:
        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()

        ((Map)data.pets).edges instanceof List
        List<Map<String, Object>> edgeList = ((Map)data.pets).edges
        edgeList[0].__typename == "DogEdge"
        ((Map)edgeList[0].node).id == "dog-1"
        ((Map)edgeList[0].node).__typename == "Dog"

    }

}
