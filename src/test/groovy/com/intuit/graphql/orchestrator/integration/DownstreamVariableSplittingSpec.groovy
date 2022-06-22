package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class DownstreamVariableSplittingSpec extends BaseIntegrationTestSpecification {

    def testSchemaA = """
        type Query {
            fieldA(objectArgA: InputA) : String
        }
        
        input InputA {
            s: String
        }
    """

    def mockServiceResponseA = [
            data: [
                    fieldA: "SomeStringA"
            ]
    ]

    def testSchemaB = """
        type Query {
            fieldB(stringArgB: String) : String
        }
    """

    def mockServiceResponseB = [
            data: [
                    fieldB: "SomeStringB"
            ]
    ]

    @Subject
    GraphQLOrchestrator specUnderTest

    def testServiceA
    def testServiceB

    void setup() {
        testServiceA = createSimpleMockService("testServiceA", testSchemaA, mockServiceResponseA)
        testServiceB = createSimpleMockService("testServiceB", testSchemaB, mockServiceResponseB)
        specUnderTest = createGraphQLOrchestrator([testServiceA, testServiceB])
    }

    def "Variables are split to different services"() {
        given:

        def graphqlQuery = '''
            query TestQuery($objectVarA: InputA, $stringVarB: String){
                fieldA(objectArgA: $objectVarA)
                fieldB(stringArgB: $stringVarB)
            }
        '''

        def variables = [
                objectVarA: [ s : "String Input" ],
                stringVarB: "StringVarValueB"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.fieldA instanceof String && data.fieldA == "SomeStringA"
        data.fieldB instanceof String && data.fieldB == "SomeStringB"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        Map<String, Object> serviceAVariables = serviceAExecutionInput.getVariables()
        serviceAVariables.size() == 1
        serviceAVariables["objectVarA"] == [ s : "String Input" ]

        ExecutionInput serviceBExecutionInput = getCapturedDownstreamExecutionInput(testServiceB)
        Map<String, Object> serviceBVariables = serviceBExecutionInput.getVariables()
        serviceBVariables.size() == 1
        serviceBVariables["stringVarB"] == "StringVarValueB"
    }

}
