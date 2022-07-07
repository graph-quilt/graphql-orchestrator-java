package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.BooksService
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.PetsService
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.UserRestService
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class QueryDirectiveSpec extends BaseIntegrationTestSpecification {

    private def petsService = new PetsService(GraphQLOrchestratorSpec.PET_ASSERTS)

    private def bookService = new BooksService(GraphQLOrchestratorSpec.BOOK_ASSERTS)

    private def userService = new UserRestService(GraphQLOrchestratorSpec.USER_ASSERTS)

    def "test Top Level Combined Schema With Include Directive On Query"() {
        given:
        ServiceProvider[] services = [ petsService, bookService, userService ]
        final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        assert orchestrator.getSchema().isSupportingMutations()

        when:
        // Test query using ExecutionInput
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query('''
            query BooksPetsAndUsers($includeType: Boolean!) { 
                books { 
                    id name 
                }
                pets { 
                    id name type @include(if: $includeType)
                }
                users { 
                    id firstName lastName @include(if: $includeType)
                } 
            }
        ''')
                .variables(ImmutableMap.of("includeType", Boolean.TRUE))
                .build()

        Map<String, Object> executionResult = orchestrator.execute(booksAndPetsEI).get()
                .toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("pets", "books", "users")
        ((List<Map<String, Objects>>) dataValue.get("pets")).size() == 3
        ((List<Map<String, Objects>>) dataValue.get("books")).size() == 3
        ((List<Map<String, Objects>>) dataValue.get("users")).size() == 3
    }

}
