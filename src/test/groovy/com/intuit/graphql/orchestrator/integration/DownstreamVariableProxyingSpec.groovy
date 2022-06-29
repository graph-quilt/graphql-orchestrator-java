package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.parser.Parser
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class DownstreamVariableProxyingSpec extends BaseIntegrationTestSpecification {

    Parser parser = new Parser()

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

    @Subject
    GraphQLOrchestrator specUnderTest

    def testServiceA

    void setup() {
        testServiceA = createSimpleMockService("testServiceA", testSchemaA, mockServiceResponseA)
        specUnderTest = createGraphQLOrchestrator(testServiceA)
    }

    def "No Variables, calls downstream with no variables"() {
        given:

        def graphqlQuery = '''
            query TestQuery {
                fieldA(objectArgA: null)
            }
        '''

        def variables = Collections.emptyMap()

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.fieldA instanceof String && data.fieldA == "SomeStringA"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        Document document = parser.parseDocument(serviceAExecutionInput.getQuery())
        OperationDefinition operationDefinition = document.getOperationDefinition("TestQuery").get()
        operationDefinition.getVariableDefinitions().size() == 0
        serviceAExecutionInput.getVariables().size() == 0
    }

    def "No Variable Definitions but with Variable Data, variable data not proxied"() {
        // the data fetching environment does not have the variable so it will not be proxied
        given:

        def graphqlQuery = '''
            query TestQuery {
                fieldA(objectArgA: null)
            }
        '''

        def variables = [
                stringArg: "StringArgValue"
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.fieldA instanceof String && data.fieldA == "SomeStringA"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        Document document = parser.parseDocument(serviceAExecutionInput.getQuery())
        OperationDefinition operationDefinition = document.getOperationDefinition("TestQuery").get()
        operationDefinition.getVariableDefinitions().size() == 0
        serviceAExecutionInput.getVariables().size() == 0
    }

    def "With variable definitions and variable data, variables are proxied"() {
        // we don't know who to send the variable data and it is unused
        given:

        def graphqlQuery = '''
            query TestQuery($varDef: InputA) {
                fieldA(objectArgA: $varDef)
            }
        '''

        def variables = [
                varDef: [ s : "String Input" ]
        ]

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.fieldA instanceof String && data.fieldA == "SomeStringA"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        Document document = parser.parseDocument(serviceAExecutionInput.getQuery())
        OperationDefinition operationDefinition = document.getOperationDefinition("TestQuery").get()
        operationDefinition.getVariableDefinitions().get(0).getName() == "varDef"
        serviceAExecutionInput.getVariables()["varDef"] == [ s : "String Input" ]
    }

    def "With variable definitions but no variable Data, variables definitions are proxied"() {
        // we don't know who to send the variable data and it is unused
        given:

        def graphqlQuery = '''
            query TestQuery($varDef: InputA) {
                fieldA(objectArgA: $varDef)
            }
        '''

        def variables = Collections.emptyMap()

        ExecutionInput executionInput = createExecutionInput(graphqlQuery, variables)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.fieldA instanceof String && data.fieldA == "SomeStringA"

        ExecutionInput serviceAExecutionInput = getCapturedDownstreamExecutionInput(testServiceA)
        Document document = parser.parseDocument(serviceAExecutionInput.getQuery())
        OperationDefinition operationDefinition = document.getOperationDefinition("TestQuery").get()
        operationDefinition.getVariableDefinitions().get(0).getName() == "varDef"
        serviceAExecutionInput.getVariables().size() == 0
    }

}
