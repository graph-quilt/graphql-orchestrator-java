package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import spock.lang.Specification

class CustomScalarsSpec extends Specification {

    def graphqlQuery = "{ uuid url }"

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

    ServiceProvider testService = new SimpleMockServiceProvider().builder()
            .sdlFiles(["schema.graphqls": testSchema])
            .mockResponse(mockServiceResponse)
            .build()

    // TODO use latest grammar
    /*
    def specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
            new AsyncExecutionStrategy(), testService)


    def "Custom Scalars can be queried"() {
        given:

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(graphqlQuery)
                .build()

        when:
        CompletableFuture<ExecutionResult> futureExecutionResult = specUnderTest.execute(executionInput)
        ExecutionResult executionResult = futureExecutionResult.get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.uuid instanceof String && data.uuid == "123e4567-e89b-12d3-a456-426614174000"
        data.url instanceof String && data.url == "https://127.0.0.1"
    }

     */
}
