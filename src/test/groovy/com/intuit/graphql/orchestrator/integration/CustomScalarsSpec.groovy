package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
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

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
    }

    def "Custom Scalars can be queried"() {
        given:

        def graphqlQuery = "{ uuid url }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.uuid instanceof String && data.uuid == "123e4567-e89b-12d3-a456-426614174000"
        data.url instanceof String && data.url == "https://127.0.0.1"
    }

}
