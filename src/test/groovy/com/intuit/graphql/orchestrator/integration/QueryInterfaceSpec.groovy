package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import com.intuit.graphql.orchestrator.utils.GraphQLUtil
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.AsyncExecutionStrategy
import graphql.language.Document
import graphql.language.FragmentDefinition
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.BaseIntegrationTestSpecification

class QueryInterfaceSpec extends BaseIntegrationTestSpecification {

    Parser parser = GraphQLUtil.parser

    private GraphQLOrchestrator specUnderTest

    def starWarsSchema = '''
        schema {
            query: QueryType
        }
        
        type QueryType {
        }
        
        extend type QueryType {
            human(id : String) : Human
            characters: [Character!]!
        }
        
        extend type QueryType {
            droid(id: ID!): Droid
        }
        
        extend type QueryType {
            hero(episode: Episode): Character
        }
        
        enum Episode {
            NEWHOPE
            EMPIRE
            JEDI
        }
        
        interface Character {
            id: ID!
            name: String!
            appearsIn: [Episode]!
            friends: [Character]
        }
        
        type Human implements Character {
            id: ID!
            name: String!
            appearsIn: [Episode]!
            friends: [Character]
            homePlanet: String
        }
        
        type Droid implements Character {
            id: ID!
            name: String!
            appearsIn: [Episode]!
            friends: [Character]
            primaryFunction: String
        }
    '''

    def setup() {
    }

    def "can Query Top Level With Result Type Of Base Interface"() {
        given:
        def mockResponse = [
                data: [
                        hero: [
                                __typename: "Droid",
                                id: "c-1",
                                name: "R2-D2",
                                appearsIn: [ "NEWHOPE", "EMPIRE", "JEDI" ],
                                primaryFunction: "Rescuing Luke"
                        ]
                ]
        ]
        SimpleMockServiceProvider starWarsService = createSimpleMockService(
                "starWarsService", starWarsSchema, mockResponse)
        specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(), null, starWarsService)
        assert !specUnderTest.getSchema().isSupportingMutations()

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query('''
            {
                hero {
                    __typename
                    ... charIdName appearsIn
                    ... on Droid
                    {
                        primaryFunction
                    }
                }
            }
            fragment charIdName on Character {
                id name
            }
        ''').build()

        when:
        Map<String, Object> executionResult = specUnderTest.execute(executionInput).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")

        dataValue.keySet().contains("hero")

        Map<String, Object> heroValue = (Map<String, Object>) dataValue.get("hero")
        heroValue.keySet().size() == 5
        heroValue.get("__typename") == "Droid"
        heroValue.get("name") == "R2-D2"
        heroValue.get("primaryFunction") == "Rescuing Luke"

        //  validate execution input variables
        ExecutionInput serviceExecutionInput = getCapturedDownstreamExecutionInput(starWarsService)
        Map<String, Object> serviceVariables = serviceExecutionInput.getVariables()
        serviceVariables.size() == 0

        def query = serviceExecutionInput.getQuery()
        query.contains("hero")
        query.contains("__type")
        query.contains("charIdName")
        query.contains("appearsIn")
        query.contains("Character")

        Document document = parser.parseDocument(serviceExecutionInput.getQuery())
        document.getDefinitions().size() == 2
        document.getFirstDefinitionOfType(OperationDefinition).get().name == "QUERY"
        document.getFirstDefinitionOfType(FragmentDefinition).get().name == "charIdName"
    }

    def "can Query Top Level With Result Type Of Interface Impl"() {
        given:
        def mockResponse = [
                data: [
                        human: [
                                name: "Obi-Wan Kenobi",
                                appearsIn: [ "NEWHOPE", "EMPIRE" ],
                                homePlanet: "Stewjon"
                        ]
                ]
        ]

        SimpleMockServiceProvider starWarsService = createSimpleMockService(
                "starWarsService", starWarsSchema, mockResponse)
        specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(), null, starWarsService)
        assert !specUnderTest.getSchema().isSupportingMutations()

        ExecutionInput executionInput = ExecutionInput
                .newExecutionInput().query('''
                    {
                        human {
                            name appearsIn homePlanet
                        }
                    }
                ''')
                .build()

        when:
        Map<String, Object> executionResult = specUnderTest.execute(executionInput).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.containsKey("human")

        Map<String, Object> human = (Map<String, Object>) dataValue.get("human")
        human.keySet().size() == 3
        human.get("name") == "Obi-Wan Kenobi"
        human.get("homePlanet") == "Stewjon"

        //  validate execution input variables
        ExecutionInput serviceExecutionInput = getCapturedDownstreamExecutionInput(starWarsService)
        Map<String, Object> serviceVariables = serviceExecutionInput.getVariables()
        serviceVariables.size() == 0

        def query = serviceExecutionInput.getQuery()
        query.contains("human")
        query.contains("name")
        query.contains("appearsIn")
        query.contains("homePlanet")

        Document document = parser.parseDocument(serviceExecutionInput.getQuery())
        document.getDefinitions().size() == 1
        document.getFirstDefinitionOfType(OperationDefinition).get().name == "QUERY"
    }

    def "can Query Top Level With Result Type Of Wrapped Interface Impl"() {
        given:
        def mockResponse = [
                data: [
                        characters: [
                                [
                                        __typename: "Human",
                                        name: "Obi-Wan Kenobi",
                                        appearsIn: [ "NEWHOPE", "EMPIRE" ]
                                ],
                                [
                                        __typename: "Droid",
                                        name: "R2-D2",
                                        appearsIn: [ "NEWHOPE", "EMPIRE", "JEDI" ]
                                ]
                        ]
                ]
        ]

        SimpleMockServiceProvider starWarsService = createSimpleMockService(
                "starWarsService", starWarsSchema, mockResponse)
        specUnderTest = createGraphQLOrchestrator(
                new AsyncExecutionStrategy(), null, starWarsService)
        assert !specUnderTest.getSchema().isSupportingMutations()

        ExecutionInput executionInput = ExecutionInput
                .newExecutionInput().query('''
                    {
                        characters {
                            name appearsIn
                        }
                    }
                ''')
                .build()

        when:
        Map<String, Object> executionResult = specUnderTest.execute(executionInput).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.containsKey("characters")

        List<Map<String, Object>> characters = (List<Map<String, Object>>) dataValue.get("characters")
        characters.size() == 2
        characters.get(0).get("name") == "Obi-Wan Kenobi"
        characters.get(1).get("name") == "R2-D2"

        //  validate execution input variables
        ExecutionInput serviceExecutionInput = getCapturedDownstreamExecutionInput(starWarsService)
        Map<String, Object> serviceVariables = serviceExecutionInput.getVariables()
        serviceVariables.size() == 0

        def query = serviceExecutionInput.getQuery()
        query.contains("characters")
        query.contains("name")
        query.contains("appearsIn")

        Document document = parser.parseDocument(serviceExecutionInput.getQuery())
        document.getDefinitions().size() == 1
        document.getFirstDefinitionOfType(OperationDefinition).get().name == "QUERY"
    }

    def "can Query Top Level With Input Variable"() {
        given:
        def mockServiceResponse = [
                data: [
                        human: [
                                name: "Obi-Wan Kenobi",
                                homePlanet: "Stewjon"
                        ]
                ]
        ]

        def starWarsService = createSimpleMockService(
                "starWarsService", starWarsSchema, mockServiceResponse)

        specUnderTest = createGraphQLOrchestrator(starWarsService)

        def graphqlQuery = '''
            query QueryHuman($humanId: String) {
                human(id: $humanId) {
                    name
                    homePlanet
                }
            }
        '''

        def variables = [
                humanId: "c-2"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()

        Map<String, Object> data = executionResult.getData()
        data != null

        Map<String, String> human = (Map<String, String>) data.get("human")
        human.get("name") == "Obi-Wan Kenobi"
        human.get("homePlanet") == "Stewjon"

        ExecutionInput serviceExecutionInput = getCapturedDownstreamExecutionInput(starWarsService)
        Map<String, Object> serviceVariables = serviceExecutionInput.getVariables()
        serviceVariables.size() == 1
        serviceVariables["humanId"] == "c-2"

        def query = serviceExecutionInput.getQuery()
        query.contains("QueryHuman")
        query.contains("human")
        query.contains("name")
    }

}
