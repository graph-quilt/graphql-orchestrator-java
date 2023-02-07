package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.MockServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import com.intuit.graphql.orchestrator.utils.GraphQLUtil
import com.intuit.graphql.orchestrator.utils.SelectionSetUtil
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.reactive.SubscriptionPublisher
import graphql.language.Document
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.BaseIntegrationTestSpecification
import reactor.core.publisher.Flux

class QueryDirectiveSpec extends BaseIntegrationTestSpecification {

    Parser parser = GraphQLUtil.parser

    GraphQLOrchestrator specUnderTest

    private def bookSchema = '''
        type Query {
            bookById(id: ID): Book
            books : [Book]
        }
        
        type Book {
            id: ID
            name: String
            pageCount: Int
            weight: Float
            isFamilyFriendly: Boolean
            author: Author
        }
        
        type Author {
            id: ID
            firstName: String
            lastName: String
            petId: String
        }
    '''

    private def petsSchema = '''
        directive @weigthFormat(role: String) on FIELD_DEFINITION
        
        directive @directiveOne(
            i: Int = 5, 
            f: Float = 6.0, 
            s: String = "DefaultString", 
            b: Boolean = true, 
            nullP: String, 
            enumP: SomeEnum = ENUMV2, 
            l: [String] = ["DefaultList1"], 
            o:DirectiveInput = { p1: "DefaultP1"}) 
        on FIELD_DEFINITION
        
        input DirectiveInput {
            p1: String
            p2: Boolean
        }
        enum SomeEnum { ENUMV1, ENUMV2 }
        
        directive @merge(if: Boolean = true) on FIELD
        
        type Query {
            pets: [Pet]
            pet(id: ID!): Pet
        }
        
        type Mutation {
            addPet(pet: InputPet!): Pet
        }
        
        type Pet{
            id: ID!
            name: String!
            age: Int 
                @weigthFormat(role: null) 
                @directiveOne(
                    i: 1 f: 2.0 s: "Yeah" b: false nullP: null 
                    enumP: ENUMV1 l: ["s1" "s2"] o: { p1: "Hello"})
            # age: Int @weigthFormat(role: null)
            weight: Weight
            purebred: Boolean
            type: AnimalType @deprecated(reason : "reason `in markdown syntax`")
        }
        
        input InputPet{
            id: ID!
            name: String!
            age: Int
            weight: Weight
            purebred: Boolean
            tag: String
        }
        
        enum AnimalType {
            DOG
            CAT
            RABBIT
        }
        
        scalar Weight
        
        directive @deprecated(reason: String = "No longer supported") on FIELD_DEFINITION | ENUM_VALUE
    '''

    private def userSchema = '''
        type Mutation {
            addUser(newUser: NewUserInput!): User!
            deleteUserById(id : ID!) : User
        }
        
        type Query {
            userById(id: ID!): User
            users: [User]
        }
        
        type User {
            id : ID!
            username : String!
            password : String!
            firstName: String!
            lastName: String!
            email: String
            phone: String
            userStatus: UserStatus
        }
        
        input NewUserInput {
            id : ID!
            username : String!
            password : String!
            firstName: String!
            lastName: String!
            email: String
            phone: String
        }
        
        enum UserStatus {
            PREACTIVE,
            ACTIVE,
            DEACTIVATED
        }
    '''

    def setup() {
    }

    def "test Top Level Combined Schema With Include Directive On Query"() {
        given:
        def bookResponse = [
                data: [
                        books: [
                                [id: "book-1", name: "GraphQL Advanced Stitching" ],
                                [id: "book-2", name: "The Recursion" ],
                                [id: "book-3", name: "Spring In Action" ]
                        ]
                ]
        ]
        SimpleMockServiceProvider bookService = createSimpleMockService("bookService", bookSchema, bookResponse)

        def petsResponse = [
                data: [
                        pets: [
                                [id: "pet-1", name: "Charlie", type: "DOG" ],
                                [id: "pet-2", name: "Milo", type: "RABBIT"],
                                [id: "pet-3", name: "Poppy", type: "CAT"]
                        ]
                ]
        ]
        SimpleMockServiceProvider petsService = createSimpleMockService("petsService", petsSchema, petsResponse)

        def userResponse = [
                data: [
                        users: [
                                [id: "user-1", firstName: "Delilah", lastName: "Hadfield" ],
                                [id: "user-2", firstName: "Huong", lastName: "Seamon" ],
                                [id: "user-3", firstName: "Geraldine", lastName: "Gower" ]
                        ]
                ]
        ]
        SimpleMockServiceProvider userService = createSimpleMockService("userService", userSchema, userResponse)

        ServiceProvider[] services = [ bookService, petsService, userService ]
        specUnderTest = createGraphQLOrchestrator(services)

        assert specUnderTest.getSchema().isSupportingMutations()

        when:
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query('''
            query BooksPetsAndUsers($includeType: Boolean!) {
                books {
                    id 
                    name
                }
                pets {
                    id 
                    name 
                    type @include(if: $includeType)
                }
                users {
                    id 
                    firstName 
                    lastName @include(if: $includeType)
                }
            }
        ''')
                .variables(ImmutableMap.of("includeType", Boolean.TRUE))
                .build()

        Map<String, Object> executionResult = specUnderTest.execute(booksAndPetsEI).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("pets", "books", "users")
        ((List<Map<String, Objects>>) dataValue.get("books")).size() == 3
        ((List<Map<String, Objects>>) dataValue.get("pets")).size() == 3
        ((List<Map<String, Objects>>) dataValue.get("users")).size() == 3

        //  petsService
        ExecutionInput petsServiceExecutionInput = getCapturedDownstreamExecutionInput(petsService)
        Map<String, Object> petsServiceVariables = petsServiceExecutionInput.getVariables()
        petsServiceVariables.size() == 1
        petsServiceVariables["includeType"] == true

        Document petsServiceDocument = parser.parseDocument(petsServiceExecutionInput.getQuery())
        OperationDefinition petsServiceOperationDefinition = petsServiceDocument.getOperationDefinition("BooksPetsAndUsers").get()
        petsServiceOperationDefinition.getVariableDefinitions().get(0).getName() == "includeType"
        petsServiceOperationDefinition.getVariableDefinitions().get(0).getType().type.name == "Boolean"

        Field pet_type_field = SelectionSetUtil.getFieldWithPath("pets.type", petsServiceOperationDefinition.getSelectionSet())
        pet_type_field.getDirectives().size() == 1
        pet_type_field.getDirectives().get(0).getName() == "include"
        pet_type_field.getDirectives().get(0).getArgument("if") != null

        //  userService
        ExecutionInput userServiceExecutionInput = getCapturedDownstreamExecutionInput(userService)
        Map<String, Object> userServiceVariables = userServiceExecutionInput.getVariables()
        userServiceVariables.size() == 1
        userServiceVariables["includeType"] == true

        Document userServiceDocument = parser.parseDocument(userServiceExecutionInput.getQuery())
        OperationDefinition userServiceOperationDefinition = userServiceDocument.getOperationDefinition("BooksPetsAndUsers").get()
        userServiceOperationDefinition.getVariableDefinitions().get(0).getName() == "includeType"
        userServiceOperationDefinition.getVariableDefinitions().get(0).getType().type.name == "Boolean"

        Field users_lastName_field = SelectionSetUtil.getFieldWithPath("users.lastName", userServiceOperationDefinition.getSelectionSet())
        users_lastName_field.getDirectives().size() == 1
        users_lastName_field.getDirectives().get(0).getName() == "include"
        users_lastName_field.getDirectives().get(0).getArgument("if") != null
    }

    def "test defer directive on Query"() {
        given:

        def initialEI = "query getPetsDeferred {pets {id name}}"
        def deferredEI = "query getPetsDeferred {pets {type __typename}}"

        def petsInitResponse = [
                data: [
                        pets: [
                                [id: "pet-1", name: "Charlie"],
                                [id: "pet-2", name: "Milo"],
                                [id: "pet-3", name: "Poppy"]
                        ]
                ]
        ]

        def petsDeferResponse = [
                data: [
                        pets: [
                                [type: "DOG", "__typename": "Pet" ],
                                [type: "RABBIT", "__typename": "Pet"],
                                [type: "CAT", "__typename": "Pet"]
                        ]
                ]
        ]
        MockServiceProvider petsService = createQueryMatchingService("petsService",
                petsSchema,
                [
                        (initialEI) :petsInitResponse,
                        (deferredEI) : petsDeferResponse
                ]
        )

        ServiceProvider[] services = [ petsService ]
        specUnderTest = createGraphQLOrchestrator(services)

        when:
        ExecutionInput petsEI = ExecutionInput.newExecutionInput()
        .query('''
            query getPetsDeferred {
                pets {
                    id 
                    name 
                    type @defer
                }
            }
        ''').build()

        ExecutionResult executionResult = specUnderTest.execute(petsEI, true).get()
        SubscriptionPublisher subscriptionPublisher = (SubscriptionPublisher)executionResult.data
        Flux<Object> publisher = (Flux) subscriptionPublisher.upstreamPublisher;
        List<ExecutionResult> results = (List<ExecutionResult>) publisher.collectList().block()

        then:
        results.size() == 2
        results.get(0).errors.size() == 0
        results.get(0).data != null

        Map<String, Object> initDataValue = (Map<String, Object>) results.get(0).data
        initDataValue.keySet().contains("pets")
        initDataValue.get("pets").size() == 3

        Map<String, Object> initPet1 = initDataValue.get("pets").get(0)
        initPet1.get("id") == "pet-1"
        initPet1.get("name") == "Charlie"
        initPet1.keySet().contains("type") == true
        initPet1.get("type") == null

        Map<String, Object> initPet2 = initDataValue.get("pets").get(1)
        initPet2.get("id") == "pet-2"
        initPet2.get("name") == "Milo"
        initPet2.keySet().contains("type") == true
        initPet2.get("type") == null

        Map<String, Object> initPet3 = initDataValue.get("pets").get(2)
        initPet3.get("id") == "pet-3"
        initPet3.get("name") == "Poppy"
        initPet3.keySet().contains("type") == true
        initPet3.get("type") == null

        Map<String, Object> deferDataValue = (Map<String, Object>) results.get(1).data
        deferDataValue.keySet().contains("pets")
        deferDataValue.get("pets").size() == 3

        Map<String, Object> deferPet1 = deferDataValue.get("pets").get(0)
        deferPet1.keySet().contains("id") == false
        deferPet1.keySet().contains("name") == false
        deferPet1.get("type") == "DOG"
        deferPet1.get("__typename") == "Pet"

        Map<String, Object> deferPet2 = deferDataValue.get("pets").get(1)
        deferPet2.keySet().contains("id") == false
        deferPet2.keySet().contains("name") == false
        deferPet2.get("type") == "RABBIT"
        deferPet2.get("__typename") == "Pet"

        Map<String, Object> deferPet3 = deferDataValue.get("pets").get(2)
        deferPet3.keySet().contains("id") == false
        deferPet3.keySet().contains("name") == false
        deferPet3.get("type") == "CAT"
        deferPet3.get("__typename") == "Pet"

    }

}
