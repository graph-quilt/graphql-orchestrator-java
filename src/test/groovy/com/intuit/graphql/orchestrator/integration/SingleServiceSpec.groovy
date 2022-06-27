package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.PetsService
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class SingleServiceSpec extends BaseIntegrationTestSpecification {

    private PetsService petsService = new PetsService(GraphQLOrchestratorSpec.PET_ASSERTS)

    void testMultipleTopLevelFieldFromSameService() {
        given:
        ServiceProvider[] services = [ petsService ]
        final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        // Test query using ExecutionInput
        ExecutionInput petsEI = ExecutionInput
                .newExecutionInput()
                .query('''
                    {
                        pets { 
                            id name 
                        }
                        pet(id : "pet-1") { 
                            id name 
                        }
                    }
                ''')
                .build()

        when:
        Map<String, Object> executionResult = orchestrator.execute(petsEI).get()
                .toSpecification()

        then:
        noExceptionThrown()

        orchestrator.getSchema().isSupportingMutations()

        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("pets", "pet")
        ((List<Map<String, Objects>>) dataValue.get("pets")).size() == 3
        ((Map<String, Objects>) dataValue.get("pet")).size() == 2
    }

}
