package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class FederationQueryWithFragmentSpec extends BaseIntegrationTestSpecification {

    GraphQLOrchestrator specUnderTest

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

    def "Query multiple field of entity extension with fragment and inline fragment"() {
        given:

        def testQuery = '''
            query  {
                bookById(id: "12345") {
                    id
                    name
                    pageCount
                    author {
                        ... authorBaseQuery
                        ... on Author {
                            lastName
                        }
                    }
                }
            }

            fragment authorBaseQuery on Author {
                idAlias:id
                fname:firstName
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

        def FNAME_ENTITY_FETCH_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {fname:firstName}}}"
        def LNAME_ENTITY_FETCH_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {lastName}}}"
        def authorResponse = [
                (FNAME_ENTITY_FETCH_QUERY): [data: [
                        _entities: [
                                [
                                        fname : "Charles"
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
        executionResult?.data?.bookById?.author.fname == "Charles"
        executionResult?.data?.bookById?.author.lastName == "Charles-LastName"

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("bookById")
        ((Map<String, Objects>) dataValue.get("bookById")).keySet().size() == 4
    }

}
