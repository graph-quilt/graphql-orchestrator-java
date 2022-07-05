package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

import java.util.function.UnaryOperator

class MultipleServicesSpec extends BaseIntegrationTestSpecification {

    private PetsService petsService = new PetsService(GraphQLOrchestratorSpec.PET_ASSERTS)

    private BooksService bookService = new BooksService(GraphQLOrchestratorSpec.BOOK_ASSERTS)

    private UserRestService userService = new UserRestService(GraphQLOrchestratorSpec.USER_ASSERTS)

    private GraphQLOrchestrator orchestrator

    def setup() {
        orchestrator = createGraphQLOrchestrator([ petsService, bookService, userService ])

        assert orchestrator.getSchema().isSupportingMutations()
    }

    def "test Top Level Combined Schema With Execution Input"() {
        given:
        // Test query using ExecutionInput
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query('''
            { 
                books { 
                    id name 
                } 
                pets { 
                    id name 
                } 
                users { 
                    id firstName lastName 
                } 
            }
        ''').build()

        Map<String, Object> executionResult = orchestrator.execute(booksAndPetsEI).get()
                .toSpecification()

        expect:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("pets", "books", "users")
        ((List<Map<String, Objects>>)dataValue.get("pets")).size() == 3
        ((List<Map<String, Objects>>)dataValue.get("books")).size() == 3
        ((List<Map<String, Objects>>)dataValue.get("users")).size() == 3
    }

    def "test Top Level Combined Schema With Execution Input Builder"() {
        given:
        // Test query using ExecutionInput.Builder
        ExecutionInput.Builder booksEIBuilder = ExecutionInput
                .newExecutionInput().query('''
                    {
                        books {
                            id name
                        }
                    }
                ''')

        expect:
        Map<String, Object> executionResult = orchestrator.execute(booksEIBuilder).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().contains("books")
        ((List<Map<String, Objects>>) dataValue.get("books")).size() == 3
    }

    def "test Top Level Combined Schema With Unary Operator"() {
        given:
        // Test query using UnaryOperator as source of query
        UnaryOperator<ExecutionInput.Builder> builderFunc = ExecutionInputTestUtil
                .builderFunc('{ "query": "{ pets { id name } }" }')

        expect:
        Map<String, Object> executionResult = orchestrator.execute(builderFunc).get().toSpecification()
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> datavalue = (Map<String, Object>) executionResult.get("data")
        datavalue.keySet().contains("pets")
        ((List<Map<String, Objects>>) datavalue.get("pets")).size() == 3
    }

}
