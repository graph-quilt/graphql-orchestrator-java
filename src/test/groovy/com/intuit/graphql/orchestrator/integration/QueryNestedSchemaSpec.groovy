package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.*
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class QueryNestedSchemaSpec extends BaseIntegrationTestSpecification {

    private GraphQLOrchestrator specUnderTest

    def personSchema = '''
        type Query {
            person : Person
        }

        type Person {
            id: ID
            name: String
            address: Address
        }

        type Address {
            id: ID
            street: String
            city: String
            zip: String
            state: String
            country: String
        }
    '''

    def bookSchema = '''
        type Query {
            person : Person
        }
        
        type Person {
            book : Book
        }
        
        type Book {
            id: ID
            name: String
            pageCount: Int
            author: Author
        }
        
        type Author {
            id: ID
            firstName: String
            lastName: String
        }

    '''
    def nestedBookSchema = '''
        extend type Author {
            pets : [Pet] @resolver(field: "person.pets")
        }
        
        type Pet {}
        
        # ================================
        # define this as built-in directive
        directive @resolver(field: String!, arguments: [ResolverArgument!]) on FIELD_DEFINITION
        
        # define this as built-in type
        input ResolverArgument {
            name : String!
            value : String!
        }
    '''

    def nestedPetsSchema = '''
        type Query {
            person : Person
        }
        
        type Person {
            pets: [Pet]
        }
        
        type Mutation {
            addPet(pet: InputPet!): Pet
        }
        
        type Pet{
            id: ID!
            name: String!
            age: Int
            weight: Float
            purebred: Boolean
            tag: String
        }
        
        input InputPet{
            id: ID!
            name: String!
            age: Int
            weight: Float
            purebred: Boolean
            tag: String
        }
    '''

    def "test Query On Nested Combined Schema"() {
        given:
        def mockPersonResponse = [
                data: [
                        person: [
                                id: "person-1",
                                name: "Kevin Whitney",
                        ]
                ]
        ]
        SimpleMockServiceProvider personService = createSimpleMockService(
                "personService", personSchema, mockPersonResponse)

        def mockBookResponse = [
                data: [
                        person: [
                                book: [ id: "book-1", name: "GraphQL Advanced Stitching" ]
                        ]
                ]
        ]
        SimpleMockServiceProvider bookService = createSimpleMockService(
                "bookService", bookSchema + " " + nestedBookSchema, mockBookResponse)

        def mockPetsResponse = [
                data: [
                        person: [
                                pets: [
                                        [ name: "Charlie" ],
                                        [ name: "Milo" ],
                                        [ name: "Poppy" ]
                                ]
                        ]
                ]
        ]
        SimpleMockServiceProvider petsService = createSimpleMockService(
                "petsService", nestedPetsSchema, mockPetsResponse)

        ServiceProvider[] services = [ personService, bookService, petsService ]
        specUnderTest  = createGraphQLOrchestrator(services)

        assert specUnderTest.getSchema().isSupportingMutations()

        ExecutionInput personEI = ExecutionInput.newExecutionInput()
                .query('''
                    {
                        person {
                            id 
                            name 
                            book {
                                id 
                                name
                            }
                            pets {
                                name
                            }
                        }
                    }
                ''')
                .build()

        when:
        Map<String, Object> executionResult = specUnderTest.execute(personEI).get().toSpecification()

        then:
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

        //  personService
        ExecutionInput personServiceExecutionInput = getCapturedDownstreamExecutionInput(personService)
        Map<String, Object> personServiceVariables = personServiceExecutionInput.getVariables()
        personServiceVariables.size() == 0

        def personQuery = personServiceExecutionInput.getQuery()
        personQuery.contains("person")
        personQuery.contains("id")
        personQuery.contains("name")

        //  bookService
        ExecutionInput bookServiceExecutionInput = getCapturedDownstreamExecutionInput(bookService)
        Map<String, Object> bookServiceVariables = bookServiceExecutionInput.getVariables()
        bookServiceVariables.size() == 0

        def bookQuery = bookServiceExecutionInput.getQuery()
        bookQuery.contains("person")
        bookQuery.contains("book")
        bookQuery.contains("id")
        bookQuery.contains("name")

        //  petsService
        ExecutionInput petsServiceExecutionInput = getCapturedDownstreamExecutionInput(petsService)
        Map<String, Object> petsServiceVariables = petsServiceExecutionInput.getVariables()
        petsServiceVariables.size() == 0

        def petsQuery = petsServiceExecutionInput.getQuery()
        petsQuery.contains("person")
        petsQuery.contains("pets")
        petsQuery.contains("name")
    }

}
