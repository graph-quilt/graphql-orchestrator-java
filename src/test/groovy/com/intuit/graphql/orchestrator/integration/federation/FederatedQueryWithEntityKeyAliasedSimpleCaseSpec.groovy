package com.intuit.graphql.orchestrator.integration.federation

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import graphql.ExecutionInput
import helpers.BaseIntegrationTestSpecification

class FederatedQueryWithEntityKeyAliasedSimpleCaseSpec extends BaseIntegrationTestSpecification {

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

    def "test Federated query single field in entity extension with entity key aliased in query"() {
        given:
        def bookResponse = [
                data: [
                        bookById: [
                                id       : "book-1",
                                name     : "GraphQuilt: The future of Schema Stitching",
                                pageCount: 100,
                                author   : [
                                        "authorId": "12345"
                                ]
                        ]

                ]
        ]

        ServiceProvider bookService = createSimpleMockService("bookService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, bookSchema, bookResponse)


        def authorResponse = [
                data: [
                        _entities: [
                                [
                                        __typename: "Author",
                                        id        : "12345",
                                        authorName : "Charles"
                                ]
                        ]
                ]
        ]
        ServiceProvider authorService = createSimpleMockService("authorService",
                ServiceProvider.ServiceType.FEDERATION_SUBGRAPH, authorSchema, authorResponse)


        ServiceProvider[] services = [bookService, authorService]
        specUnderTest = createGraphQLOrchestrator(services)

        when:
        ExecutionInput booksAndPetsEI = ExecutionInput.newExecutionInput().query('''
            query  {
                bookById(id: "12345") {
                    id 
                    name
                    pageCount
                    author {
                        authorId: id
                        authorName: firstName
                    }
                }
            }
        ''')
                .variables(ImmutableMap.of("includeType", Boolean.TRUE))
                .build()

        Map<String, Object> executionResult = specUnderTest.execute(booksAndPetsEI).get().toSpecification()

        then:

        def AUTHOR_QUERY = "query (\$REPRESENTATIONS:[_Any!]!) {_entities(representations:\$REPRESENTATIONS) {... on Author {authorName:firstName}}}"

        executionResult.get("errors") == null
        executionResult.get("data") != null

        executionResult?.data?.bookById?.id == "book-1"
        executionResult?.data?.bookById?.name == "GraphQuilt: The future of Schema Stitching"
        executionResult?.data?.bookById?.pageCount == 100
        executionResult?.data?.bookById?.author.authorId == "12345"
        executionResult?.data?.bookById?.author.authorName == "Charles"

        Map<String, Object> dataValue = (Map<String, Object>) executionResult.get("data")
        dataValue.keySet().containsAll("bookById")
        ((Map<String, Objects>) dataValue.get("bookById")).keySet().size() == 4
        ExecutionInput authorServiceExecutionInput = getCapturedDownstreamExecutionInput(authorService)
        authorServiceExecutionInput.getQuery() == AUTHOR_QUERY
        Map<String, Object> authorsServiceVariables = authorServiceExecutionInput.getVariables()
        authorsServiceVariables?.REPRESENTATIONS[0]?.__typename == "Author"
        authorsServiceVariables?.REPRESENTATIONS[0]?.id == "12345"
    }

}
