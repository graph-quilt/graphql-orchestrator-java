package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class CustomScalarsSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        type Query {
            uuid: UUID
            url: URL
        }
        
        scalar UUID @specifiedBy(url: "https://tools.ietf.org/html/rfc4122")
        scalar URL @specifiedBy(url: "https://tools.ietf.org/html/rfc3986")
    """

    def mockServiceResponse = [
            data: [
                    uuid: "123e4567-e89b-12d3-a456-426614174000",
                    url: "https://127.0.0.1"
            ]
    ]

    GraphQLSchema graphQLSchema

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
        graphQLSchema = specUnderTest.getSchema()
    }

    def "can build schema with custom scalars"() {
        given: "graphQLSchema"

        and:
        GraphQLScalarType scalarType = (GraphQLScalarType) graphQLSchema.getType("UUID")
        GraphQLDirective graphQLDirective = (GraphQLDirective) scalarType.getDirective("specifiedBy")
        GraphQLArgument graphQLArgument = graphQLDirective.getArgument("url")
        String actualUrlArg = (String) graphQLArgument.getArgumentValue().getValue()

        expect:
        actualUrlArg == "https://tools.ietf.org/html/rfc4122"
    }

    def "Custom Scalars can be queried"() {
        given:

        def graphqlQuery = "{ uuid url }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(null,
                "query QUERY { uuid url }", (SimpleMockServiceProvider) testService)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.uuid instanceof String && data.uuid == "123e4567-e89b-12d3-a456-426614174000"
        data.url instanceof String && data.url == "https://127.0.0.1"
    }

}
