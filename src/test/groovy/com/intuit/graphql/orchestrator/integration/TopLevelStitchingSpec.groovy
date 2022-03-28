package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.TestHelper
import graphql.ExecutionInput
import graphql.ExecutionResult
import helpers.BaseIntegrationTestSpecification
import spock.lang.Subject

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

    def "Person service is stitched and queried"() {
        given:
        specUnderTest = createGraphQLOrchestrator([epsService, personService])

        def graphqlQuery = "{ person { name id income } }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
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
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.Profile?.prefFirstName instanceof String && data.Profile?.prefFirstName == "First Name"
        data.Profile?.prefLastName instanceof String && data.Profile?.prefLastName == "Last Name"
        data.Profile?.version instanceof Integer && data.Profile?.version == Short.MAX_VALUE
    }

    def "Eps service is stitched and mutation query is successful"() {
        given:
        specUnderTest = createGraphQLOrchestrator([epsMutationService, personService])

        def graphqlQuery = "mutation { upsertProfile(corpId: \"test corp\", input: {version: ${Short.MAX_VALUE}}) { prefFirstName prefLastName version } }"

        ExecutionInput executionInput = createExecutionInput(graphqlQuery)

        when:
        ExecutionResult executionResult = specUnderTest.execute(executionInput).get()

        then:
        executionResult.getErrors().isEmpty()
        Map<String, Object> data = executionResult.getData()
        data.upsertProfile?.prefFirstName instanceof String && data.upsertProfile?.prefFirstName == "First Name"
        data.upsertProfile?.prefLastName instanceof String && data.upsertProfile?.prefLastName == "Last Name"
        data.upsertProfile?.version instanceof Integer && data.upsertProfile?.version == Short.MAX_VALUE
    }
}
