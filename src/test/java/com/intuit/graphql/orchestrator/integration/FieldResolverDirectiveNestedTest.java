package com.intuit.graphql.orchestrator.integration;

import static com.intuit.graphql.orchestrator.GraphQLOrchestratorTest.createGraphQLOrchestrator;
import static com.intuit.graphql.orchestrator.testhelpers.CustomAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.TestHelper;
import com.intuit.graphql.orchestrator.testhelpers.ExecutionInputMatcher;
import com.intuit.graphql.orchestrator.testhelpers.MockServiceProvider;
import com.intuit.graphql.orchestrator.testhelpers.ServiceProviderMockResponse;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.execution.AsyncExecutionStrategy;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class FieldResolverDirectiveNestedTest {

  private static final String PERSON_DOWNSTREAM_QUERY = "query Get_Person {person {id} person {name}}";

  private static final String BOOK_DOWNSTREAM_QUERY = "query Get_Person {person {book {id name author {lastName}}}}";

  private static final String PETS_DOWNSTREAM_QUERY = "query Get_Person {person {pets(animalType:DOG,pureBred:true) {name}}}";

  private static final String PETS_FIELD_RESOLVER_DOWNSTREAM_QUERY = "query Get_Person_Resolver_Directive_Query {person {pets_0:pets(animalType:DOG,pureBred:true) {name}}}";


  private ExecutionInput personEI;
  private ExecutionInput bookEI;
  private ExecutionInput petsEI;
  private ExecutionInput petsFieldResolverEI;

  private ServiceProvider mockPersonService;
  private ServiceProvider mockBookService;
  private ServiceProvider mockPetsService;

  @Before
  public void setup() throws IOException {

    personEI = ExecutionInput.newExecutionInput()
        .query(PERSON_DOWNSTREAM_QUERY)
        .build();

    bookEI = ExecutionInput.newExecutionInput()
        .query(BOOK_DOWNSTREAM_QUERY)
        .build();

    petsEI = ExecutionInput.newExecutionInput()
        .query(PETS_DOWNSTREAM_QUERY)
        .build();

    petsFieldResolverEI  = ExecutionInput.newExecutionInput()
        .query(PETS_FIELD_RESOLVER_DOWNSTREAM_QUERY)
        .build();

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

  @Test
  public void testFieldResolverInNestedField() throws Exception {
    // GIVEN
    ServiceProvider[] services = new ServiceProvider[]{mockPersonService, mockBookService, mockPetsService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
        new AsyncExecutionStrategy(), services);
    assertThat(orchestrator.getSchema().isSupportingMutations()).isTrue();

    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query("query Get_Person { person { id name book {id name author {lastName pets { name } } } pets(animalType: DOG, pureBred: true) { name }  } }")
        .build();

    // WHEN
    ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockPersonService, times(1))
        .query(argThat(new ExecutionInputMatcher(personEI)), any(GraphQLContext.class));
    verify(mockBookService, times(1))
        .query(argThat(new ExecutionInputMatcher(bookEI)), any(GraphQLContext.class));
    verify(mockPetsService, times(1))
        .query(argThat(new ExecutionInputMatcher(petsEI)), any(GraphQLContext.class));
    verify(mockPetsService, times(1))
        .query(argThat(new ExecutionInputMatcher(petsFieldResolverEI)), any(GraphQLContext.class));

    assertThat(executionResult).hasNoErrors();
    assertThat(executionResult).pathHasArraySize("$.data.person.pets", 3);
    assertThat(executionResult).pathEquals("$.data.person.book.id", "book-1");
    assertThat(executionResult).pathEquals("$.data.person.book.name", "GraphQL Advanced Stitching");

    // service link
    assertThat(executionResult).pathEquals("$.data.person.book.author.pets[0].name", "Charlie");
    assertThat(executionResult).pathEquals("$.data.person.book.author.pets[2].name", "Poppy");
  }

}