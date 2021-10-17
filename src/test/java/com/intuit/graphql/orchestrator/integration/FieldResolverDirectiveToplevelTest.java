package com.intuit.graphql.orchestrator.integration;

import static com.intuit.graphql.orchestrator.GraphQLOrchestratorTest.createGraphQLOrchestrator;
import static com.intuit.graphql.orchestrator.testhelpers.CustomAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import com.intuit.graphql.orchestrator.GraphQLOrchestrator;
import com.intuit.graphql.orchestrator.ServiceProvider;
import com.intuit.graphql.orchestrator.ServiceProvider.ServiceType;
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
public class FieldResolverDirectiveToplevelTest {

  private static final String PET_BY_ID_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} "
      + "query GetQuery_Resolver_Directive_Query {pet_0:pet(id:\"pet-1\") {...petFragment} pet_1:pet(id:\"pet-2\") {...petFragment} pet_2:pet(id:\"pet-3\") {...petFragment}}";

  private static final String PETS_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} query GetQuery {pets {...petFragment type}}";

  private static final String BOOKS_DOWNSTREAM_QUERY = "fragment bookFragment on Book {id name author {__typename lastName petId} pageCount weight isFamilyFriendly} query GetQuery {books {...bookFragment}}";

  private static final String USERS_DOWNSTREAM_QUERY = "query GetQuery {users {id firstName lastName}}";

  private static final String BOOKS_SIMPLE_DOWNSTREAM_QUERY = "query GetQuery {books {id name author {firstName}}}";

  private static final String USERS_WITH_LINK_DOWNSTREAM_QUERY = "query GetQuery {userById(id:\"user-1\") {id firstName lastName petId1 petId2}}";

  private static final String PETBYID_1_DOWNSTREAM_QUERY = "query GetQuery_Resolver_Directive_Query {petById1_0:petById1(id:\"pet-1\") {... on Cat {name catBreed} ... on Dog {name dogBreed} __typename}}";

  private static final String PETBYID_2_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} query GetQuery_Resolver_Directive_Query {petById2_0:petById2(id:\"pet-2\") {...petFragment __typename}}";

  private ExecutionInput petByIdEI;
  private ExecutionInput petsEI;
  private ExecutionInput booksEI;
  private ExecutionInput usersEI;
  private ExecutionInput booksSimpleEI;
  private ExecutionInput userWithLinkByIdEI;
  private ExecutionInput petById1EI;
  private ExecutionInput petById2EI;

  private ServiceProvider mockPetsService;
  private ServiceProvider mockPetsServiceWithErrors;
  private ServiceProvider mockPetsServiceWithErrorsNoPath;
  private ServiceProvider mockBookService;
  private ServiceProvider mockBookServiceMissingFieldLink;
  private ServiceProvider mockUserService;

  private ServiceProvider mockUserWithLinkService;
  private ServiceProvider mockPetsWithInterfaceUnionService;

  @Before
  public void setup() throws IOException {

    petByIdEI = ExecutionInput.newExecutionInput()
        .query(PET_BY_ID_DOWNSTREAM_QUERY)
        .build();

    petsEI = ExecutionInput.newExecutionInput()
        .query(PETS_DOWNSTREAM_QUERY)
        .build();

    booksEI = ExecutionInput.newExecutionInput()
        .query(BOOKS_DOWNSTREAM_QUERY)
        .build();

    booksSimpleEI = ExecutionInput.newExecutionInput()
        .query(BOOKS_SIMPLE_DOWNSTREAM_QUERY)
        .build();

    usersEI = ExecutionInput.newExecutionInput()
        .query(USERS_DOWNSTREAM_QUERY)
        .build();

    userWithLinkByIdEI = ExecutionInput.newExecutionInput()
        .query(USERS_WITH_LINK_DOWNSTREAM_QUERY)
        .build();

    petById1EI = ExecutionInput.newExecutionInput()
        .query(PETBYID_1_DOWNSTREAM_QUERY)
        .build();

    petById2EI = ExecutionInput.newExecutionInput()
        .query(PETBYID_2_DOWNSTREAM_QUERY)
        .build();

    mockPetsService = MockServiceProvider.builder()
        .namespace("PETS")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/books-and-pets/schema-pets.graphqls"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-petById-1to3.json")
            .forExecutionInput(petByIdEI)
            .build())
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-pets.json")
            .forExecutionInput(petsEI)
            .build())
        .build();

    mockPetsServiceWithErrors = MockServiceProvider.builder()
        .namespace("PETS")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/books-and-pets/schema-pets.graphqls"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-petById-1to3-with-error.json")
            .forExecutionInput(petByIdEI)
            .build())
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-pets.json")
            .forExecutionInput(petsEI)
            .build())
        .build();

    mockPetsServiceWithErrorsNoPath = MockServiceProvider.builder()
        .namespace("PETS")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/books-and-pets/schema-pets.graphqls"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-petById-1to3-with-error-no-paths.json")
            .forExecutionInput(petByIdEI)
            .build())
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-pets.json")
            .forExecutionInput(petsEI)
            .build())
        .build();

    mockBookService = MockServiceProvider.builder()
        .namespace("BOOKS")
        .sdlFiles(TestHelper.getFileMapFromList(
            "top_level/books-and-pets/schema-books.graphqls",
            "top_level/books-and-pets/pet-author-link.graphqls"))
        .domainTypes(ImmutableSet.of("Book", "Author", "NonExistingType"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/books-and-pets/mock-responses/get-books.json")
            .forExecutionInput(booksEI)
            .build())
        .build();

    mockBookServiceMissingFieldLink = MockServiceProvider.builder()
        .namespace("BOOKS")
        .sdlFiles(TestHelper.getFileMapFromList(
            "top_level/books-and-pets/schema-books.graphqls",
            "top_level/books-and-pets/pet-author-link.graphqls"))
        .domainTypes(ImmutableSet.of("Book", "Author", "NonExistingType"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse(
                "top_level/books-and-pets/mock-responses/get-books-missing-field-link.json")
            .forExecutionInput(booksSimpleEI)
            .build())
        .build();

    mockUserService = MockServiceProvider.builder()
        .namespace("USER")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/user/user-schema.graphqls"))
        .serviceType(ServiceType.REST)
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/user/mock-responses/get-users.json")
            .forExecutionInput(usersEI)
            .build())
        .build();

    mockUserWithLinkService = MockServiceProvider.builder()
        .namespace("USER")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/user-and-pets/user-schema.graphqls",
            "top_level/user-and-pets/user-pet-link.graphqls"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
            .forExecutionInput(userWithLinkByIdEI)
            .build())
        .build();

    mockPetsWithInterfaceUnionService = MockServiceProvider.builder()
        .namespace("PETS")
        .sdlFiles(TestHelper.getFileMapFromList("top_level/user-and-pets/schema-pets.graphqls"))
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/user-and-pets/mock-responses/get-petById-1.json")
            .forExecutionInput(petById1EI)
            .build())
        .mockResponse(ServiceProviderMockResponse.builder()
            .expectResponse("top_level/user-and-pets/mock-responses/get-petById-2.json")
            .forExecutionInput(petById2EI)
            .build())
        .build();
  }

  @Test
  public void testFieldResolver() throws Exception {
    // GIVEN
    ServiceProvider[] services = new ServiceProvider[]{mockBookService, mockPetsService, mockUserService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);

    String queryString = "query GetQuery { __schema { types { name } } "
        + "books { ... bookFragment } pets { ... petFragment type }  users { id firstName lastName } } "
        + "fragment bookFragment on BOOKS_Book { id name  author { __typename lastName petId pet { ... petFragment }} pageCount weight isFamilyFriendly } "
        + "fragment petFragment on Pet { id name }";
    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query(queryString)
        .build();

    // WHEN
   ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockPetsService, times(1))
        .query(argThat(new ExecutionInputMatcher(petsEI)), any(GraphQLContext.class));
    verify(mockPetsService, times(1))
        .query(argThat(new ExecutionInputMatcher(petByIdEI)), any(GraphQLContext.class));
    verify(mockBookService, times(1))
        .query(argThat(new ExecutionInputMatcher(booksEI)), any(GraphQLContext.class));
    verify(mockUserService, times(1))
        .query(argThat(new ExecutionInputMatcher(usersEI)), any(GraphQLContext.class));

    assertThat(executionResult).hasNoErrors();
    assertThat(executionResult).pathContainsKeys("$.data", "pets","books","users");
    assertThat(executionResult).pathHasArraySize("$.data.books", 3);
    assertThat(executionResult).pathHasArraySize("$.data.pets", 3);
    assertThat(executionResult).pathHasArraySize("$.data.users", 3);
    assertThat(executionResult).pathEquals("$.data.books[0].id", "book-1");
    assertThat(executionResult).pathEquals("$.data.books[2].id", "book-3");
    assertThat(executionResult).pathEquals("$.data.pets[0].name", "Charlie");
    assertThat(executionResult).pathEquals("$.data.pets[2].name", "Poppy");
    assertThat(executionResult).pathEquals("$.data.users[0].firstName", "Delilah");
    assertThat(executionResult).pathEquals("$.data.users[2].firstName", "Geraldine");

    // service link
    assertThat(executionResult).pathEquals("$.data.books[0].author.pet.name", "Charlie");
    assertThat(executionResult).pathEquals("$.data.books[1].author.pet.name", "Milo");
    assertThat(executionResult).pathEquals("$.data.books[2].author.pet.name", "Poppy");
  }

  @Test
  public void testFieldResolverWihInterfaceAndUnion() throws Exception {
    // GIVEN
    ServiceProvider[] services = new ServiceProvider[]{mockUserWithLinkService, mockPetsWithInterfaceUnionService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);

    String queryString = "query GetQuery { "
        + "userById(id: \"user-1\") { id firstName lastName petId1 pet1 { ... on Cat { name catBreed } ... on Dog { name dogBreed } } petId2 pet2 { ... petFragment } } } "
        + "fragment petFragment on Pet { id name }";

    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query(queryString)
        .build();

    // WHEN
    ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockUserWithLinkService, times(1))
        .query(argThat(new ExecutionInputMatcher(userWithLinkByIdEI)), any(GraphQLContext.class));
    verify(mockPetsWithInterfaceUnionService, times(1))
        .query(argThat(new ExecutionInputMatcher(petById1EI)), any(GraphQLContext.class));
    verify(mockPetsWithInterfaceUnionService, times(1))
        .query(argThat(new ExecutionInputMatcher(petById2EI)), any(GraphQLContext.class));

    assertThat(executionResult).hasNoErrors();
    assertThat(executionResult).pathEquals("$.data.userById.firstName", "Delilah");
    assertThat(executionResult).pathEquals("$.data.userById.lastName", "Hadfield");

    // service link
    assertThat(executionResult).pathEquals("$.data.userById.pet1.name", "Lassie");
    assertThat(executionResult).pathEquals("$.data.userById.pet1.dogBreed", "COLLIE");
    assertThat(executionResult).pathEquals("$.data.userById.pet2.id", "pet-2");
    assertThat(executionResult).pathEquals("$.data.userById.pet2.name", "Garfield");
  }

  @Test
  public void testFieldResolverInTopLevelFieldPetServiceWithErrors() throws Exception {
    // GIVEN
    ServiceProvider[] services = new ServiceProvider[]{mockBookService, mockPetsServiceWithErrors, mockUserService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);

    String queryString = "query GetQuery { __schema { types { name } } "
        + "books { ... bookFragment } pets { ... petFragment type }  users { id firstName lastName } } "
        + "fragment bookFragment on BOOKS_Book { id name  author { __typename lastName petId pet { ... petFragment }} pageCount weight isFamilyFriendly } "
        + "fragment petFragment on Pet { id name }";
    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query(queryString)
        .build();

    // WHEN
    ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockPetsServiceWithErrors, times(1))
        .query(argThat(new ExecutionInputMatcher(petsEI)), any(GraphQLContext.class));
    verify(mockPetsServiceWithErrors, times(1))
        .query(argThat(new ExecutionInputMatcher(petByIdEI)), any(GraphQLContext.class));
    verify(mockBookService, times(1))
        .query(argThat(new ExecutionInputMatcher(booksEI)), any(GraphQLContext.class));
    verify(mockUserService, times(1))
        .query(argThat(new ExecutionInputMatcher(usersEI)), any(GraphQLContext.class));

    assertThat(executionResult).hasErrors();
    assertThat(executionResult).pathHasArraySize("$.errors", 3);
    assertThat(executionResult).pathContains("$.errors[0].message", "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service.");
    assertThat(executionResult).pathContains("$.errors[0].extensions.downstreamErrors.message", "Exception while fetching data (/pet_0) : Hello Error!");
    assertThat(executionResult).pathContains("$.errors[1].extensions.downstreamErrors.message", "Exception while fetching data (/pet_1/age) : Null Pointer Exception");
    assertThat(executionResult).pathContains("$.errors[2].message", "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service.");
    assertThat(executionResult).pathContains("$.errors[2].extensions.downstreamErrors.message", "Exception while fetching data (/pet_2) : Hello Error!");

    assertThat(executionResult).pathContainsKeys("$.data", "pets","books","users");
    assertThat(executionResult).pathHasArraySize("$.data.books", 3);
    assertThat(executionResult).pathHasArraySize("$.data.pets", 3);
    assertThat(executionResult).pathHasArraySize("$.data.users", 3);
    assertThat(executionResult).pathEquals("$.data.books[0].id", "book-1");
    assertThat(executionResult).pathEquals("$.data.books[2].id", "book-3");
    assertThat(executionResult).pathEquals("$.data.pets[0].name", "Charlie");
    assertThat(executionResult).pathEquals("$.data.pets[2].name", "Poppy");
    assertThat(executionResult).pathEquals("$.data.users[0].firstName", "Delilah");
    assertThat(executionResult).pathEquals("$.data.users[2].firstName", "Geraldine");

    // service link
    assertThat(executionResult).pathIsNull("$.data.books[0].author.pet");
    assertThat(executionResult).pathEquals("$.data.books[1].author.pet.name", "Milo");
    assertThat(executionResult).pathIsNull("$.data.books[2].author.pet");
  }

  @Test
  public void testFieldResolverInTopLevelFieldPetServiceWithErrorsButNoPath() throws Exception {
    // GIVEN
    ServiceProvider[] services = new ServiceProvider[]{mockBookService, mockPetsServiceWithErrorsNoPath, mockUserService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);

    String queryString = "query GetQuery { __schema { types { name } } "
        + "books { ... bookFragment } pets { ... petFragment type }  users { id firstName lastName } } "
        + "fragment bookFragment on BOOKS_Book { id name  author { __typename lastName petId pet { ... petFragment }} pageCount weight isFamilyFriendly } "
        + "fragment petFragment on Pet { id name }";
    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query(queryString)
        .build();

    // WHEN
    ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockPetsServiceWithErrorsNoPath, times(1))
        .query(argThat(new ExecutionInputMatcher(petsEI)), any(GraphQLContext.class));
    verify(mockPetsServiceWithErrorsNoPath, times(1))
        .query(argThat(new ExecutionInputMatcher(petByIdEI)), any(GraphQLContext.class));
    verify(mockBookService, times(1))
        .query(argThat(new ExecutionInputMatcher(booksEI)), any(GraphQLContext.class));
    verify(mockUserService, times(1))
        .query(argThat(new ExecutionInputMatcher(usersEI)), any(GraphQLContext.class));

    assertThat(executionResult).hasErrors();
    assertThat(executionResult).pathHasArraySize("$.errors", 3);
    assertThat(executionResult).pathContains("$.errors[0].message", "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service.");
    assertThat(executionResult).pathContains("$.errors[0].extensions.downstreamErrors.message", "Exception while fetching data (/pet_0) : Hello Error!");
    assertThat(executionResult).pathIsNotFound("$.errors[0].extensions.downstreamErrors.path");

    assertThat(executionResult).pathContains("$.errors[1].extensions.downstreamErrors.message", "Exception while fetching data (/pet_1/age) : Null Pointer Exception");
    assertThat(executionResult).pathHasArraySize("$.errors[1].extensions.downstreamErrors.path", 0);

    assertThat(executionResult).pathContains("$.errors[2].message", "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service.");
    assertThat(executionResult).pathContains("$.errors[2].extensions.downstreamErrors.message", "Exception while fetching data (/pet_2) : Hello Error!");
    assertThat(executionResult).pathIsNotFound("$.errors[2].extensions.downstreamErrors.path");

    assertThat(executionResult).pathContainsKeys("$.data", "pets","books","users");
    assertThat(executionResult).pathHasArraySize("$.data.books", 3);
    assertThat(executionResult).pathHasArraySize("$.data.pets", 3);
    assertThat(executionResult).pathHasArraySize("$.data.users", 3);
    assertThat(executionResult).pathEquals("$.data.books[0].id", "book-1");
    assertThat(executionResult).pathEquals("$.data.books[2].id", "book-3");
    assertThat(executionResult).pathEquals("$.data.pets[0].name", "Charlie");
    assertThat(executionResult).pathEquals("$.data.pets[2].name", "Poppy");
    assertThat(executionResult).pathEquals("$.data.users[0].firstName", "Delilah");
    assertThat(executionResult).pathEquals("$.data.users[2].firstName", "Geraldine");

    // service link
    assertThat(executionResult).pathIsNull("$.data.books[0].author.pet");
    assertThat(executionResult).pathEquals("$.data.books[1].author.pet.name", "Milo");
    assertThat(executionResult).pathIsNull("$.data.books[2].author.pet");
  }

  @Test
  public void testFieldResolverLinkFieldMissingInQuery() throws Exception {
    // GIVEN
    String testContext = "fieldName=petId,  "
        + "parentTypeName=BOOKS_Author,  "
        + "resolverDirectiveDefinition=ResolverDirectiveDefinition(field=pet, arguments=[ResolverArgumentDefinition(name=id, value=$petId)]), "
        + "serviceNameSpace=BOOKS";

    String expectedError1 = "Field not found in parent's resolved value.  " + testContext;
    String expectedError2 = "Field not found in parent's resolved value.  " + testContext;
    String expectedError3 = "Field not found in parent's resolved value.  " + testContext;

    ServiceProvider[] services = new ServiceProvider[]{mockBookServiceMissingFieldLink, mockPetsService};
    final GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(
        new AsyncExecutionStrategy(), null, services);

    ExecutionInput query = ExecutionInput.newExecutionInput()
        .query("query GetQuery { books { id name  author { firstName pet { id name } } } }")
        .build();

    // WHEN
    ExecutionResult executionResult = orchestrator.execute(query).get();

    // THEN
    verify(mockBookServiceMissingFieldLink, times(1)).query(argThat(new ExecutionInputMatcher(booksSimpleEI)), any(GraphQLContext.class));

    assertThat(executionResult).hasErrors();
    assertThat(executionResult).pathContains("$.errors[0].message", expectedError1);
    assertThat(executionResult).pathContains("$.errors[1].message", expectedError2);
    assertThat(executionResult).pathContains("$.errors[2].message", expectedError3);

    assertThat(executionResult).pathContainsKeys("$.data", "books");
    assertThat(executionResult).pathHasArraySize("$.data.books", 3);
    assertThat(executionResult).pathEquals("$.data.books[0].id", "book-1");
    assertThat(executionResult).pathEquals("$.data.books[2].id", "book-3");

    // service link
    assertThat(executionResult).pathIsNull("$.data.books[0].author.pet");
    assertThat(executionResult).pathIsNull("$.data.books[1].author.pet");
    assertThat(executionResult).pathIsNull("$.data.books[2].author.pet");
  }

}