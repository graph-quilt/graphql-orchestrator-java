package helpers

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.RuntimeGraph
import com.intuit.graphql.orchestrator.stitching.SchemaStitcher
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.ExecutionIdProvider
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.parser.Parser
import spock.lang.Specification

class BaseIntegrationTestSpecification extends Specification {

    public static final Parser PARSER = new Parser()

    def testService

    def createSimpleMockService(String testSchema, Map<String, Object> mockServiceResponse) {
        return new SimpleMockServiceProvider().builder()
                .sdlFiles(["schema.graphqls": testSchema])
                .mockResponse(mockServiceResponse)
                .build()
    }

    def createSimpleMockService(String namespace, String testSchema, Map<String, Object> mockServiceResponse) {
        return new SimpleMockServiceProvider().builder()
                .sdlFiles(["schema.graphqls": testSchema])
                .namespace(namespace)
                .mockResponse(mockServiceResponse)
                .build()
    }

    static GraphQLOrchestrator createGraphQLOrchestrator(ServiceProvider service) {
        RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().service(service)
                .build().stitchGraph()

        GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator()
        builder.runtimeGraph(runtimeGraph)
        builder.instrumentations(Collections.emptyList())
        builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
        builder.queryExecutionStrategy(new AsyncExecutionStrategy())
        builder.mutationExecutionStrategy(new AsyncExecutionStrategy())
        return builder.build()
    }

    static GraphQLOrchestrator createGraphQLOrchestrator(List<ServiceProvider> services) {
        RuntimeGraph runtimeGraph = SchemaStitcher.newBuilder().services(services)
                .build().stitchGraph()

        GraphQLOrchestrator.Builder builder = GraphQLOrchestrator.newOrchestrator()
        builder.runtimeGraph(runtimeGraph)
        builder.instrumentations(Collections.emptyList())
        builder.executionIdProvider(ExecutionIdProvider.DEFAULT_EXECUTION_ID_PROVIDER);
        builder.queryExecutionStrategy(new AsyncExecutionStrategy())
        builder.mutationExecutionStrategy(new AsyncExecutionStrategy())
        return builder.build()
    }

    static ExecutionInput createExecutionInput(String graphqlQuery, Map<String, Object> variables) {
        ExecutionInput.newExecutionInput()
                .query(graphqlQuery)
                .variables(variables)
                .build()
    }

    static ExecutionInput createExecutionInput(String graphqlQuery) {
        ExecutionInput.newExecutionInput()
                .query(graphqlQuery)
                .variables(Collections.emptyMap())
                .build()
    }

    ExecutionInput getCapturedDownstreamExecutionInput() {
        return testService.getExecutionInputArgumentCaptor().getValue()
    }

    Object toDocument(String query) {
        return PARSER.parseDocument(query)
    }

    OperationDefinition getQueryOperationDefinition(Document document) {
        return document.getDefinitions().stream()
                .filter({ definition -> definition instanceof OperationDefinition })
                .map({ definition -> (OperationDefinition) definition })
                .filter({ operationDefinition -> operationDefinition.getOperation() == OperationDefinition.Operation.QUERY })
                .findFirst().get();

    }
}
