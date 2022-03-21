package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import spock.lang.Specification

class InterfaceImplementInterfaceSpec extends Specification {

    def graphqlQuery = """
        query {
            pets {
               edges {
                    ... on DogEdge {
                        node {
                            id name isServiceDog
                        }
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
                            edges     : [
                                    node      : [
                                            id          : "dog-1",
                                            name        : "Lassie",
                                            isServiceDog: false,
                                            __typename  : "Dog"
                                    ],
                                    __typename: "DogEdge"
                            ],
                            __typename: "DogConnection"
                    ]
            ]
    ]

    ServiceProvider testService = new SimpleMockServiceProvider().builder()
            .sdlFiles(["schema.graphqls": testSchema])
            .mockResponse(mockServiceResponse)
            .build()

// TODO this is currently failing and may need changes on the grammar
/*
    def specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
            new AsyncExecutionStrategy(), testService)

    def "interface can extends another interface"() {
        given:
        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()

    }
*/
}
