package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class DownstreamQueryOptimizingSpec extends BaseIntegrationTestSpecification {

    def testSchemaA = """
        type Query {
            a : A
        }
        
        type A {
            b: B
            c: C
        }
        
        type B {
            leaf : String
        }
        
        type C {
            leaf : String
        }
    """

    def mockServiceResponseA = [
            data: [
                    a: [ b : [leaf :"somethingB"], c : [leaf:"somethingC"]]
            ]
    ]

    def testSchemaB = """
        type Query {
            a : A
        }
 
        type A {
            d : D
        }
        
        type D{
            leaf : String
        }
    """

    def mockServiceResponseB = [
            data: [
                    a: [d : [leaf:"somethingD"]]
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
            query TestQuery{
                a {
                    c {
                        leaf
                    }
                    b {
                        leaf
                    }
                    d {
                      leaf
                      }
                  }
            }
        '''

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        Map<String,Object> valA = data.a;
        Map<String, Object> valB = valA.b;
        Map<String, Object> valC = valA.c;
        Map<String, Object> valD = valA.d;
        valB.leaf instanceof String && valB.leaf == "somethingB"
        valC.leaf instanceof String && valC.leaf == "somethingC"
        valD.leaf instanceof String && valD.leaf == "somethingD"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        serviceAExecutionInput.query == 'query TestQuery {a {c {leaf} b {leaf}}}'

        ExecutionInput serviceBExecutionInput = getCapturedDownstreamExecutionInput(testServiceB)
        serviceBExecutionInput.query == 'query TestQuery {a {d {leaf}}}'
    }

}
