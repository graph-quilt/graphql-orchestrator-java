package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class FederationQueryWithFieldSelectionsOnlySpec extends BaseIntegrationTestSpecification {

    GraphQLOrchestrator specUnderTest

    static testQueryBase = '''
            query  {
                bookById(id: "12345") {
                    id 
                    name
                    pageCount
                    author {
                        id
                        firstName
                    }
                }
            }
        '''

    static testQueryKeyAliased = '''
            query  {
                bookById(id: "12345") {
                    id
                    name
                    pageCount
                    author {
                        idAlias: id
                        fnameAlias: firstName
                    }
                }
            }
        '''

    static testQueryKeyNotAliased = '''
            query  {
                bookById(id: "12345") {
                    id 
                    name
                    pageCount
                    author {
                        id
                        fnameAlias: firstName
                    }
                }
            }
        '''

    static testQueryKeyNotQueried = '''
            query  {
                bookById(id: "12345") {
                    id 
                    name
                    pageCount
                    author {
                        fnameAlias: firstName
                    }
                }
            }
        '''

    private def bookSchema = '''
        type Query {
            bookById(id: ID): Book
        }
        
        type Book {
            id: ID
            name: String
            pageCount: Int
            author: Author
        }    
        
        type Author @key(fields: "id") {
            id: String
        }    
    '''

    private def authorSchema = '''
        type Query {
            authorByid(id: ID!): Author
        }
        
        type Author @extends @key(fields: "id") {
            id: String @external
            firstName: String!
            lastName: String!
        }      
    '''

    def setup() {
    }

    def "Query single field of entity extension"(testQuery, bookSvcAuthorData, author_id, author_idAlias, author_firstName, author_fnameAlias) {
        given:

        def bookResponse = [
                data: [
                        bookById: [
                                id       : "book-1",
                                name     : "GraphQuilt: The future of Schema Stitching",
                                pageCount: 100,
                                author   : bookSvcAuthorData
                        ]

                ]
        ]

        def fnameField = author_firstName != null ? "firstName" : "fnameAlias"
        def authorResponse = [
                data: [
                        _entities: [
                                [
                                        (fnameField): "Charles"
                                ]
                        ]
                ]
        ]

        ServiceProvider bookService = createSimpleMockService("bookService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, bookSchema, bookResponse)


        ServiceProvider authorService = createSimpleMockService("authorService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, authorSchema, authorResponse)


        ServiceProvider[] services = [bookService, authorService]
        specUnderTest = createGraphQLOrchestrator(services)

        when:
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query(testQuery)
                .variables(ImmutableMap.of("includeType", Boolean.TRUE))
                .build()

        Map<String, Object> executionResult = specUnderTest.execute(booksAndPetsEI).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        executionResult?.data?.bookById?.id == "book-1"
        executionResult?.data?.bookById?.name == "GraphQuilt: The future of Schema Stitching"
        executionResult?.data?.bookById?.pageCount == 100

        executionResult?.data?.bookById?.author.id == author_id
        executionResult?.data?.bookById?.author.idAlias == author_idAlias
        executionResult?.data?.bookById?.author.firstName == author_firstName
        executionResult?.data?.bookById?.author.fnameAlias == author_fnameAlias

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("bookById")
        ((Map<String, Objects>) dataValue.get("bookById")).keySet().size() == 4
        ExecutionInput authorServiceExecutionInput = getCapturedDownstreamExecutionInput(authorService)

        def firstNameSelection = author_firstName != null ? "firstName" : "fnameAlias:firstName"
        authorServiceExecutionInput.getQuery() == "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {${firstNameSelection}}}}"
        Map<String, Object> authorsServiceVariables = authorServiceExecutionInput.getVariables()
        authorsServiceVariables?.REPRESENTATIONS[0]?.__typename == "Author"
        authorsServiceVariables?.REPRESENTATIONS[0]?.id == "12345"

        where:
        testQuery              | bookSvcAuthorData    | author_id | author_idAlias | author_firstName | author_fnameAlias
        testQueryBase          | ["id": "12345"]      | "12345"   | null           | "Charles"        | null
        testQueryKeyAliased    | ["idAlias": "12345"] | null      | "12345"        | null             | "Charles"
        testQueryKeyNotAliased | ["id": "12345"]      | "12345"   | null           | null             | "Charles"
        testQueryKeyNotQueried | ["id": "12345"]      | null      | null           | null             | "Charles"

    }

    def "Query multiple field of entity extension"() {
        given:

        def testQuery = '''
            query  {
                bookById(id: "12345") {
                    id 
                    name
                    pageCount
                    author {
                        idAlias:id
                        firstName
                        lastName
                    }
                }
            }
        '''

        def bookResponse = [
                data: [
                        bookById: [
                                id       : "book-1",
                                name     : "GraphQuilt: The future of Schema Stitching",
                                pageCount: 100,
                                author   : ["idAlias": "12345"]
                        ]

                ]
        ]

        def FNAME_ENTITY_FETCH_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {firstName}}}"
        def LNAME_ENTITY_FETCH_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {lastName}}}"
        def authorResponse = [
                (FNAME_ENTITY_FETCH_QUERY): [data: [
                        _entities: [
                                [
                                        firstName : "Charles"
                                ]
                        ]
                ]],
                (LNAME_ENTITY_FETCH_QUERY): [data: [
                        _entities: [
                                [
                                        lastName  : "Charles-LastName"
                                ]
                        ]
                ]]

        ]

        ServiceProvider bookService = createSimpleMockService("bookService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, bookSchema, bookResponse)


        ServiceProvider authorService = createQueryMatchingService("authorService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, authorSchema, authorResponse)


        ServiceProvider[] services = [bookService, authorService]
        specUnderTest = createGraphQLOrchestrator(services)

        when:
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query(testQuery)
                .variables(ImmutableMap.of("includeType", Boolean.TRUE))
                .build()

        Map<String, Object> executionResult = specUnderTest.execute(booksAndPetsEI).get().toSpecification()

        then:
        executionResult.get("errors") == null
        executionResult.get("data") != null

        executionResult?.data?.bookById?.id == "book-1"
        executionResult?.data?.bookById?.name == "GraphQuilt: The future of Schema Stitching"
        executionResult?.data?.bookById?.pageCount == 100

        executionResult?.data?.bookById?.author.idAlias == "12345"
        executionResult?.data?.bookById?.author.firstName == "Charles"
        executionResult?.data?.bookById?.author.lastName == "Charles-LastName"

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("bookById")
        ((Map<String, Objects>) dataValue.get("bookById")).keySet().size() == 4
    }

}
