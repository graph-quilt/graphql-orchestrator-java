package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class MutationNestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {
    def mockDServiceResponse = [
            data: [
                    serviceD: []
            ]
    ]

    def mockEServiceResponse = [
            data: [
                    serviceD: []
            ]
    ]

    def "cannot Build Due To Mutation Nested Fields Has Mismatched Arguments"() {
        def serviceD = createSimpleMockService( "SVCD", "type Query { } "
                + "type Mutation { container(in : String) : Container } "
                + "type Container { serviceD : ServiceD } "
                + "type ServiceD { svcDField1 : String }", mockDServiceResponse)

        def serviceE = createSimpleMockService( "SVCE", "type Query { } "
                + "type Mutation { container(out : String) : Container } "
                + "type Container { serviceE : ServiceE } "
                + "type ServiceE { svcEField1 : String }", mockEServiceResponse)


        given:
        ServiceProvider[] services = [ serviceD, serviceE ]

        when:
        GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        then:
        thrown(FieldMergeException)

        orchestrator == null
    }
}
