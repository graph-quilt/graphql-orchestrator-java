package com.intuit.graphql.orchestrator.integration

import com.google.common.collect.ImmutableMap
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.TestHelper
import com.intuit.graphql.orchestrator.TestServiceProvider
import com.intuit.graphql.orchestrator.schema.Operation
import com.intuit.graphql.orchestrator.stitching.StitchingException
import com.intuit.graphql.orchestrator.testhelpers.SimpleMockServiceProvider
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

import static com.intuit.graphql.orchestrator.VirtualOrchestratorProvider.FIELD_NAME
import static com.intuit.graphql.orchestrator.VirtualOrchestratorProvider.ORCHESTRATOR

class TopLevelStitchingSpec extends BaseIntegrationTestSpecification {
    def epsService, epsMutationService, personService

    def mockPersonServiceResponse = [
            data: [
                    person: [
                        id: "Test Id",
                        income: Short.MAX_VALUE,
                        name: "Test Name"
                    ]
            ]
    ]

    def mockEpsServiceResponse = [
            data: [
                    Profile: [
                        prefFirstName: "First Name",
                        prefLastName: "Last Name",
                        version: Short.MAX_VALUE
                    ]
            ]
    ]

    def mockEpsServiceMutationResponse = [
            data: [
                    upsertProfile: [
                        prefFirstName: "First Name",
                        prefLastName: "Last Name",
                        version: Short.MAX_VALUE
                    ]
            ]
    ]

    @Subject
    def specUnderTest

    void setup() {
        epsService = createSimpleMockService( "EPS", TestHelper.getResourceAsString("top_level/eps/schema2.graphqls"), mockEpsServiceResponse)
        epsMutationService = createSimpleMockService( "EPS", TestHelper.getResourceAsString("top_level/eps/schema2.graphqls"), mockEpsServiceMutationResponse)
        personService = createSimpleMockService("PERSON", TestHelper.getResourceAsString("top_level/person/schema1.graphqls"), mockPersonServiceResponse)
    }

    def "Orchestrator works without any downstream service provider"() {
        given:
        specUnderTest = createGraphQLOrchestrator([])

        def graphqlQuery = "{ ${FIELD_NAME} }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data?._namespace instanceof String && data?._namespace == ORCHESTRATOR

    }

    def "Person service is stitched and queried"() {
        given:
        specUnderTest = createGraphQLOrchestrator([epsService, personService])

        def graphqlQuery = "{ person { name id income } }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(Operation.QUERY, graphqlQuery, (SimpleMockServiceProvider) personService)
        compareQueryToExecutionInput(null, null, (SimpleMockServiceProvider) epsService)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.person?.name instanceof String && data.person?.name == "Test Name"
        data.person?.id instanceof String && data.person?.id == "Test Id"
        data.person?.income instanceof Integer && data.person?.income == Short.MAX_VALUE
    }

    def "Eps service is stitched and queried"() {
        given:
        specUnderTest = createGraphQLOrchestrator([epsService, personService])

        def graphqlQuery = "{ Profile(corpId: \"test corp\") { prefFirstName prefLastName version } }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(Operation.QUERY, graphqlQuery, (SimpleMockServiceProvider) epsService)
        compareQueryToExecutionInput(null, null, (SimpleMockServiceProvider) personService)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.Profile?.prefFirstName instanceof String && data.Profile?.prefFirstName == "First Name"
        data.Profile?.prefLastName instanceof String && data.Profile?.prefLastName == "Last Name"
        data.Profile?.version instanceof Integer && data.Profile?.version == Short.MAX_VALUE

    }

    def "Eps service is stitched and mutation query is successful"() {
        given:
        specUnderTest = createGraphQLOrchestrator([epsMutationService, personService])

        def baseGraphqlQuery = "{ upsertProfile(corpId: \"test corp\", input: {version: ${Short.MAX_VALUE}}) { prefFirstName prefLastName version } }"
        def graphqlQuery = "mutation " + baseGraphqlQuery

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        compareQueryToExecutionInput(Operation.MUTATION, baseGraphqlQuery, (SimpleMockServiceProvider) epsMutationService)
        compareQueryToExecutionInput(null, null, (SimpleMockServiceProvider) personService)
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.upsertProfile?.prefFirstName instanceof String && data.upsertProfile?.prefFirstName == "First Name"
        data.upsertProfile?.prefLastName instanceof String && data.upsertProfile?.prefLastName == "Last Name"
        data.upsertProfile?.version instanceof Integer && data.upsertProfile?.version == Short.MAX_VALUE
    }

    def "Empty provider cannot stitch"() {
        given:
        def emptyProvider = TestServiceProvider.newBuilder()
                .namespace("Empty")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of())
                .build()

        when:
        createGraphQLOrchestrator(emptyProvider)

        then:
        thrown(StitchingException.class)
    }

    def "Provider with empty file cannot stitch successfully"() {
        given:
        def emptyProvider = TestServiceProvider.newBuilder()
                .namespace("Empty")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("schema.graphqls", ""))
                .build()

        when:
        createGraphQLOrchestrator(emptyProvider)

        then:
        thrown(StitchingException.class)
    }

    def "Provider with only empty operation cannot stitch successfully"() {
        given:
        String schema = "type Query {} type Mutation {} type Subscription {}"
        def emptyProvider = TestServiceProvider.newBuilder()
                .namespace("Empty")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("schema.graphqls", schema))
                .build()

        when:
        createGraphQLOrchestrator(emptyProvider)

        then:
        thrown(StitchingException.class)
    }

    def "Provider with empty operation and extending operation can stitch"() {
        given:

        String schema = "type Query {} extend type Query {getID: ID}"
        def emptyProvider = TestServiceProvider.newBuilder()
                .namespace("Empty")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("schema.graphqls", schema))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator(emptyProvider)

        then:
        specUnderTest != null
    }

    def "Non-Federation graph with only extending types fails to stitch"() {
        given:
        String schema = "type entityA @extends @key(fields\"a\") { a: String @external b: ID }"
        def emptyProvider = TestServiceProvider.newBuilder()
                .namespace("BAD")
                .serviceType(ServiceProvider.ServiceType.GRAPHQL)
                .sdlFiles(ImmutableMap.of("schema.graphqls", schema))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator(emptyProvider)

        then:
        thrown(StitchingException.class)
    }

    def "Federation graph with only extending types can stitch"() {
        given:
        String schema = "type Query { getA: entityA } type entityA @key(fields:\"a\") { a:String }"
        String extSchema = "type entityA @extends @key(fields:\"a\") { a: String @external b: ID }"
        def baseProvider = TestServiceProvider.newBuilder()
                .namespace("BASE")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema.graphqls", schema))
                .build()

        def extProvider = TestServiceProvider.newBuilder()
                .namespace("EXT")
                .serviceType(ServiceProvider.ServiceType.FEDERATION_SUBGRAPH)
                .sdlFiles(ImmutableMap.of("schema.graphqls", extSchema))
                .build()

        when:
        specUnderTest = createGraphQLOrchestrator(baseProvider, extProvider)

        then:
        notThrown(StitchingException.class)
        specUnderTest != null
    }
}
