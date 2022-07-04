package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class QueryNestedSchemaSpec extends BaseIntegrationTestSpecification {

    void testQueryOnNestedCombinedSchema() {
        given:
        ServiceProvider[] services = [
                new PersonService(), new NestedBooksService(), new NestedPetsService() ]
        final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        assert orchestrator.getSchema().isSupportingMutations()

        when:
        // Test query using ExecutionInput
        ExecutionInput personEI = ExecutionInput.newExecutionInput()
            .query('''
                { 
                    person { 
                        id name book {
                            id name
                        } 
                        pets { 
                            name 
                        } 
                    } 
                }
            ''')
            .build()

        then:
        Map<String, Object> executionResult = orchestrator.execute(personEI).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.get("person") != null

        Map<String, Object> personVALUE = (Map<String, Object>) dataValue.get("person")
        personVALUE.get("pets") != null

        List<Map<String, Object>> petsValue = (List<Map<String, Object>>) personVALUE.get("pets")
        petsValue.size() == 3
        personVALUE.get("book") != null

        Map<String, Object> bookValue = (Map<String, Object>) personVALUE.get("book")
        bookValue.get("id") == "book-1"
        bookValue.get("name") == "GraphQL Advanced Stitching"
    }

}
