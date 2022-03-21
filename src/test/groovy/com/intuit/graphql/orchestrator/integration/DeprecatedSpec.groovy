package com.intuit.graphql.orchestrator.integration

import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class DeprecatedSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        type Query {
            x(arg1 : String @deprecated, arg2: InputType) : String
        }
        
        input InputType {
            y: String @deprecated
        }  
    """

    def mockServiceResponse = [
            data: [
                    x: "some value for x"
            ]
    ]

    @Subject
    def specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
    }

    def "can query fields where type definition has @deprecated on Argument and Input fields"() {
        given:
        def graphqlQuery = '''
            query TestQuery($a1: String, $a2: InputType) {
                x(arg1: $a1, arg2: $a2)
            }
        '''

        def variables =  [
            a1: "some string",
            a2: [
                    y: "Y"
            ]
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.x instanceof String && data.x == "some value for x"
    }

}
