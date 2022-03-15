package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.AsyncExecutionStrategy
import graphql.language.Directive
import graphql.language.OperationDefinition
import helpers.BaseIntegrationTestSpecification
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static com.intuit.graphql.orchestrator.GraphQLOrchestratorTest.createGraphQLOrchestrator

class DirectivesOnVariableDefinitionSpec extends Specification {

    def graphqlQuery = '''
        query TestQuery($stringVar: String @directiveOnVar(dirArg : "dirArgValue")){
            field(stringArg: $stringVar)
        }
    '''

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

    SimpleMockServiceProvider testService = new SimpleMockServiceProvider().builder()
            .sdlFiles(["schema.graphqls": testSchema])
            .mockResponse(mockServiceResponse)
            .build()

    def specUnderTest = createGraphQLOrchestrator(new AsyncExecutionStrategy(),
            new AsyncExecutionStrategy(), testService)


    def "directives are allowed in variable definitions"() {
        given:

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(graphqlQuery)
                .variables([ stringVar: "argValue"])
                .build()

        when:
        CompletableFuture<ExecutionResult> futureExecutionResult = specUnderTest.execute(executionInput)
        ExecutionResult executionResult = futureExecutionResult.get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.field instanceof String && data.field == "SomeString"

        ExecutionInput actualExecutionInputArgument = testService.getExecutionInputArgumentCaptor().getValue()
        String actualDownstreamQuery = actualExecutionInputArgument.getQuery()
        def document = BaseIntegrationTestSpecification.PARSER.parseDocument(actualDownstreamQuery)
        OperationDefinition operationDef  = document.getDefinitions().get(0)
        List<Directive> actualDirectives = operationDef.getVariableDefinitions().get(0)
                .getDirectives("directiveOnVar")
        actualDirectives.size() == 1
    }

}
