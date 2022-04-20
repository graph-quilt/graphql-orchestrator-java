package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.testhelpers.MockServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.ServiceProviderMockResponse
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class FieldResolverDirectiveNestedSpec extends BaseIntegrationTestSpecification {
    def PERSON_DOWNSTREAM_QUERY = "query Get_Person {person {id} person {name}}";
    def BOOK_DOWNSTREAM_QUERY = "query Get_Person {person {book {id name author {lastName}}}}";
    def PETS_DOWNSTREAM_QUERY = "query Get_Person {person {pets {name}}}";
    def PETS_FIELD_RESOLVER_DOWNSTREAM_QUERY = "query Get_Person_Resolver_Directive_Query {person {pets_0:pets {name}}}";

    def personEI, bookEI, petsEI, petsFieldResolverEI
    def mockPersonService,mockBookService ,mockPetsService

    @Subject
    specUnderTest

    void setup() {
        personEI = ExecutionInput.newExecutionInput().query(PERSON_DOWNSTREAM_QUERY).build();
        bookEI = ExecutionInput.newExecutionInput().query(BOOK_DOWNSTREAM_QUERY).build();
        petsEI = ExecutionInput.newExecutionInput().query(PETS_DOWNSTREAM_QUERY).build();
        petsFieldResolverEI  = ExecutionInput.newExecutionInput().query(PETS_FIELD_RESOLVER_DOWNSTREAM_QUERY).build();

        mockPersonService = MockServiceProvider.builder()
                .namespace("PERSON")
                .sdlFiles(TestHelper.getFileMapFromList("nested/books-pets-person/schema-person.graphqls"))
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/person/mock-responses/get-person-1.json")
                        .forExecutionInput(personEI)
                        .build())
                .build();

        mockBookService = MockServiceProvider.builder()
                .namespace("BOOKS")
                .sdlFiles(TestHelper.getFileMapFromList(
                        "nested/books-pets-person/schema-books.graphqls",
                        "nested/books-pets-person/pet-author-link.graphqls"
                ))
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("nested/books-pets-person/mock-responses/get-books.json")
                        .forExecutionInput(bookEI)
                        .build())
                .build();

        mockPetsService = MockServiceProvider.builder()
                .namespace("PETS")
                .sdlFiles(TestHelper.getFileMapFromList("nested/books-pets-person/schema-pets.graphqls"))
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("nested/books-pets-person/mock-responses/get-pets.json")
                        .forExecutionInput(petsEI)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse(
                                "nested/books-pets-person/mock-responses/get-pets-via-field-resolver.json")
                        .forExecutionInput(petsFieldResolverEI)
                        .build())
                .build();
    }

    def "testFieldResolverInNestedField"() {
        given:
        specUnderTest = createGraphQLOrchestrator([mockPersonService, mockBookService, mockPetsService]);

        ExecutionInput query = ExecutionInput.newExecutionInput()
                .query("query Get_Person { person { id name book {id name author {lastName pets { name } } } pets { name }  } }")
                .build();

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get();

        then:
        executionResult?.errors?.size() == 0
        executionResult?.data?.person?.pets?.size() == 3
        executionResult?.data?.person?.book?.id == "book-1"
        executionResult?.data?.person?.book?.name == "GraphQL Advanced Stitching"

        executionResult?.data?.person?.book?.author?.pets[0].name == "Charlie"
        executionResult?.data?.person?.book?.author?.pets[2].name == "Poppy"
    }
}
