package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import graphql.ExecutionInput
import graphql.execution.AsyncExecutionStrategy
import helpers.BaseIntegrationTestSpecification

class QueryInterfaceSpec extends BaseIntegrationTestSpecification {

    private ServiceProvider[] services

    private GraphQLOrchestrator orchestrator

    def setup() {
        services = [ new StarWarsService() ]

        orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(), null, services)

        assert !orchestrator.getSchema().isSupportingMutations()
    }

    def "can Query Toplevel With Result Type Of Base Interface"() {
        when:
        // Test query using ExecutionInput
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

        then:
        Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().contains("hero")

        Map<String, Object> heroValue = (Map<String, Object>) dataValue.get("hero")
        heroValue.keySet().size() == 5
        heroValue.get("__typename") == "Droid"
        heroValue.get("name") == "R2-D2"
        heroValue.get("primaryFunction") == "Rescuing Luke"
    }

    def "can Query Toplevel With Result Type Of Interface Impl"() {
        when:
        // Test query using ExecutionInput
        ExecutionInput executionInput = ExecutionInput
                .newExecutionInput().query('''
                {
                    human {
                        name appearsIn homePlanet
                    } 
                }
            ''')
                .build()

        then:
        Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.containsKey("human")

        Map<String, Object> human = (Map<String, Object>) dataValue.get("human")
        human.keySet().size() == 3
        human.get("name") == "Obi-Wan Kenobi"
        human.get("homePlanet") == "Stewjon"
    }

    def "can Query Toplevel With Result Type Of Wrapped Interface Impl"() {
        when:
        // Test query using ExecutionInput
        ExecutionInput executionInput = ExecutionInput
                .newExecutionInput().query('''
                    {
                        characters { 
                            name appearsIn
                        }
                    }
                ''')
                .build()

        then:
        Map<String, Object> executionResult = orchestrator.execute(executionInput).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.containsKey("characters")

        List<Map<String, Object>> characters = (List<Map<String, Object>>) dataValue.get("characters")
        characters.size() == 2
        characters.get(0).get("name") == "Obi-Wan Kenobi"
        characters.get(1).get("name") == "R2-D2"
    }

}
