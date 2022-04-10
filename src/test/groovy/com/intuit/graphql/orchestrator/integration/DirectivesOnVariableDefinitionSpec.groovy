package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.language.Directive
import graphql.language.OperationDefinition
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

class DirectivesOnVariableDefinitionSpec extends BaseIntegrationTestSpecification {

    def testSchema = """
        directive @directiveOnVar(dirArg : String) on VARIABLE_DEFINITION
 
        type Query {
            field(stringArg: String) : String
        }
    """

    def mockServiceResponse = [
            data: [
                    field: "SomeString"
            ]
    ]

    @Subject
    GraphQLOrchestrator specUnderTest

    void setup() {
        testService = createSimpleMockService(testSchema, mockServiceResponse)
        specUnderTest = createGraphQLOrchestrator(testService)
    }

    def "directives are allowed in variable definitions"() {
        given:

        def graphqlQuery = '''
            query TestQuery($stringVar: String @directiveOnVar(dirArg : "dirArgValue")){
                field(stringArg: $stringVar)
            }
        '''

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.field instanceof String && data.field == "SomeString"

        ExecutionInput captureddownstreamExecutionInput = getCapturedDownstreamExecutionInput()
        String actualDownstreamQuery = captureddownstreamExecutionInput.getQuery()
        def document = toDocument(actualDownstreamQuery)
        OperationDefinition queryOperationDef  = getQueryOperationDefinition(document)
        List<Directive> actualDirectives = queryOperationDef.getVariableDefinitions().get(0)
                .getDirectives("directiveOnVar")
        actualDirectives.size() == 1
    }

}
