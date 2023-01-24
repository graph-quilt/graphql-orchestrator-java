package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableSet
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.testhelpers.MockServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.ServiceProviderMockResponse
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class FieldResolverDirectiveTopLevelSpec extends BaseIntegrationTestSpecification {

    String PET_BY_ID_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} " + \
     "query GetQuery_Resolver_Directive_Query {pet_0:pet(id:\"pet-1\") {...petFragment} pet_1:pet(id:\"pet-2\") {...petFragment} pet_2:pet(id:\"pet-3\") {...petFragment}}";

    def PETS_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} query GetQuery {pets {...petFragment type}}"

    def BOOKS_DOWNSTREAM_QUERY = "fragment bookFragment on Book {id name author {__typename lastName petId} pageCount weight isFamilyFriendly} query GetQuery {books {...bookFragment}}"

    def USERS_DOWNSTREAM_QUERY = "query GetQuery {users {id firstName lastName}}"

    def BOOKS_SIMPLE_DOWNSTREAM_QUERY = "query GetQuery {books {id name author {}}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY = "query GetQuery {userById(id:\"user-1\") {id firstName petId1}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY2 = "query GetQuery {userById(id:\"user-1\") {id petId1 firstName}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY3 = "query GetQuery {userById(id:\"user-1\") {firstName id petId1}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY4 = "query GetQuery {userById(id:\"user-1\") {firstName id petId1}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY5 = "query GetQuery {userById(id:\"user-1\") {petId1 id firstName}}"

    def USERS_WITH_LINK_DOWNSTREAM_QUERY6 = "query GetQuery {userById(id:\"user-1\") {petId1 firstName id}}"

    def PETBYID_1_DOWNSTREAM_QUERY = "query GetQuery_Resolver_Directive_Query {petById1_0:petById1(id:\"pet-1\") {... on Cat {name catBreed} ... on Dog {name dogBreed} __typename}}"

    def PETBYID_2_DOWNSTREAM_QUERY = "fragment petFragment on Pet {id name} query GetQuery_Resolver_Directive_Query {petById2_0:petById2(id:\"pet-2\") {...petFragment __typename}}"

    def petsEI, petByIdEI, petById1EI, petById2EI
    def usersEI, userWithLinkByIdEI,userWithLinkByIdEI2,userWithLinkByIdEI3,userWithLinkByIdEI4,userWithLinkByIdEI5,userWithLinkByIdEI6
    def booksEI, booksSimpleEI

    ServiceProvider mockPetsService, mockPetsServiceWithErrors, mockPetsServiceWithErrorsNoPath
    ServiceProvider mockBookService, mockBookServiceMissingFieldLink
    ServiceProvider mockUserService, mockUserWithLinkService, mockPetsWithInterfaceUnionService

    @Subject
    def specUnderTest

    void setup() {

        petByIdEI = ExecutionInput.newExecutionInput().query(PET_BY_ID_DOWNSTREAM_QUERY).build()

        petsEI = ExecutionInput.newExecutionInput().query(PETS_DOWNSTREAM_QUERY).build()

        booksEI = ExecutionInput.newExecutionInput().query(BOOKS_DOWNSTREAM_QUERY).build()

        booksSimpleEI = ExecutionInput.newExecutionInput().query(BOOKS_SIMPLE_DOWNSTREAM_QUERY).build()

        usersEI = ExecutionInput.newExecutionInput().query(USERS_DOWNSTREAM_QUERY).build()

        userWithLinkByIdEI = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY).build()

        userWithLinkByIdEI2 = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY2).build()

        userWithLinkByIdEI3 = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY3).build()

        userWithLinkByIdEI4 = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY4).build()

        userWithLinkByIdEI5 = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY5).build()

        userWithLinkByIdEI6 = ExecutionInput.newExecutionInput().query(USERS_WITH_LINK_DOWNSTREAM_QUERY6).build()

        petById1EI = ExecutionInput.newExecutionInput().query(PETBYID_1_DOWNSTREAM_QUERY).build()

        petById2EI = ExecutionInput.newExecutionInput().query(PETBYID_2_DOWNSTREAM_QUERY).build()

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
                .build()

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
                .build()

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
                .build()

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
                .build()

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
                .build()

        mockUserService = MockServiceProvider.builder()
                .namespace("USER")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/user/user-schema.graphqls"))
                .serviceType(ServiceProvider.ServiceType.REST)
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user/mock-responses/get-users.json")
                        .forExecutionInput(usersEI)
                        .build())
                .build()

        mockUserWithLinkService = MockServiceProvider.builder()
                .namespace("USER")
                .sdlFiles(TestHelper.getFileMapFromList("top_level/user-and-pets/user-schema.graphqls",
                        "top_level/user-and-pets/user-pet-link.graphqls"))
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI2)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI3)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI4)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI5)
                        .build())
                .mockResponse(ServiceProviderMockResponse.builder()
                        .expectResponse("top_level/user-and-pets/mock-responses/get-userById-1.json")
                        .forExecutionInput(userWithLinkByIdEI6)
                        .build())
                .build()

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
                .build()
    }

    def "testFieldResolver"() {
        given:
        specUnderTest = createGraphQLOrchestrator([mockBookService, mockPetsService, mockUserService])

        String queryString = """query GetQuery { 
        __schema { types { name } }
        books { ... bookFragment } 
        pets { ... petFragment type }  
        users { id firstName lastName } }
        fragment bookFragment on BOOKS_Book { 
             id name 
             author { 
                __typename lastName petId pet { ... petFragment }
             } 
             pageCount weight isFamilyFriendly 
         }
        fragment petFragment on Pet { id name }"""

        ExecutionInput query = ExecutionInput.newExecutionInput().query(queryString).build()

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get()

        then:
        executionResult.getErrors().isEmpty()
        executionResult?.data?.books[0]?.id == "book-1"
        executionResult?.data?.books[2]?.id == "book-3"
        executionResult?.data?.pets[0]?.name == "Charlie"
        executionResult?.data?.pets[2]?.name == "Poppy"
        executionResult?.data?.users[0]?.firstName == "Delilah"
        executionResult?.data?.users[2]?.firstName == "Geraldine"

        executionResult?.data?.books[0]?.author?.pet?.name == "Charlie"
        executionResult?.data?.books[1]?.author?.pet?.name == "Milo"
        executionResult?.data?.books[2]?.author?.pet?.name == "Poppy"
    }

    def "testFieldResolverWihInterfaceAndUnion"() {
        given:
        specUnderTest = createGraphQLOrchestrator([mockUserWithLinkService, mockPetsWithInterfaceUnionService])

        String queryString = """query GetQuery {
            userById(id: "user-1") { 
                id firstName petId1 
                pet1 { 
                    ... on Cat { name catBreed } 
                    ... on Dog { name dogBreed } 
                }  
               
            } 
        }
       """

        ExecutionInput query = ExecutionInput.newExecutionInput().query(queryString).build()

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get()

        then:
        executionResult.getErrors().isEmpty()
        executionResult?.data?.userById?.firstName == "Delilah"
        executionResult?.data?.userById?.pet1?.name == "Lassie"
        executionResult?.data?.userById?.pet1?.dogBreed == "COLLIE"
    }

    def "testFieldResolverInTopLevelFieldPetServiceWithErrors"() {
        given:
        specUnderTest = createGraphQLOrchestrator([mockBookService, mockPetsServiceWithErrors, mockUserService])

        String queryString = """query GetQuery { 
            __schema { types { name } } 
            books { ... bookFragment } 
            pets { ... petFragment type } 
            users { id firstName lastName } 
        }
        fragment bookFragment on BOOKS_Book { id name  
        author { __typename lastName petId pet { ... petFragment }} pageCount weight isFamilyFriendly }
        fragment petFragment on Pet { id name }"""

        ExecutionInput query = ExecutionInput.newExecutionInput().query(queryString).build()

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get()

        then:
        executionResult.getErrors().size() == 3
        executionResult?.errors[0]?.message == "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service."
        executionResult?.errors[0]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_0) : Hello Error!"
        executionResult?.errors[1]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_1/age) : Null Pointer Exception"
        executionResult?.errors[1]?.extensions?.serviceNamespace == "PETS"
        executionResult?.errors[2]?.message == "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service."
        executionResult?.errors[2]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_2) : Hello Error!"
        executionResult?.errors[2]?.extensions?.serviceNamespace == "PETS"

        executionResult?.data?.books?.size() == 3
        executionResult?.data?.pets?.size() == 3
        executionResult?.data?.users?.size() == 3
        executionResult?.data?.books[0]?.id == "book-1"
        executionResult?.data?.books[2]?.id == "book-3"
        executionResult?.data?.pets[0]?.name == "Charlie"
        executionResult?.data?.pets[2]?.name == "Poppy"
        executionResult?.data?.users[0]?.firstName == "Delilah"
        executionResult?.data?.users[2]?.firstName == "Geraldine"

        executionResult?.data?.books[0]?.author?.pet == null
        executionResult?.data?.books[1]?.author?.pet?.name == "Milo"
        executionResult?.data?.books[2]?.author?.pet == null
    }

    def "testFieldResolverInTopLevelFieldPetServiceWithErrorsButNoPath"() {
        given:
        specUnderTest = createGraphQLOrchestrator([mockBookService, mockPetsServiceWithErrorsNoPath, mockUserService])

        String queryString = """query GetQuery { __schema { types { name } }
        books { ... bookFragment } pets { ... petFragment type }  users { id firstName lastName } }
        fragment bookFragment on BOOKS_Book { id name  author { __typename lastName petId pet { ... petFragment }} pageCount weight isFamilyFriendly }
        fragment petFragment on Pet { id name }"""
        ExecutionInput query = ExecutionInput.newExecutionInput().query(queryString).build()

        when:
        ExecutionResult executionResult = specUnderTest.execute(query).get()

        then:
        executionResult?.errors?.size() == 3
        executionResult?.errors[0]?.message == "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service."
        executionResult?.errors[0]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_0) : Hello Error!"
        executionResult?.errors[0]?.extensions?.downstreamErrors?.path == null

        executionResult?.errors[1]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_1/age) : Null Pointer Exception"
        executionResult?.errors[1]?.extensions?.downstreamErrors?.path?.size()  == 0

        executionResult?.errors[2]?.message == "FieldResolverDirectiveDataFetcher encountered an error while calling downstream service."
        executionResult?.errors[2]?.extensions?.downstreamErrors?.message == "Exception while fetching data (/pet_2) : Hello Error!"
        executionResult?.errors[2]?.extensions?.downstreamErrors?.path == null

        executionResult?.data?.books?.size() == 3
        executionResult?.data?.pets?.size() == 3
        executionResult?.data?.users?.size() == 3
        executionResult?.data?.books[0]?.id == "book-1"
        executionResult?.data?.books[2]?.id == "book-3"
        executionResult?.data?.pets[0]?.name == "Charlie"
        executionResult?.data?.pets[2]?.name == "Poppy"
        executionResult?.data?.users[0]?.firstName == "Delilah"
        executionResult?.data?.users[2]?.firstName == "Geraldine"

        executionResult?.data?.books[0]?.author?.pet == null
        executionResult?.data?.books[1]?.author?.pet?.name == "Milo"
        executionResult?.data?.books[2]?.author?.pet == null
    }

}