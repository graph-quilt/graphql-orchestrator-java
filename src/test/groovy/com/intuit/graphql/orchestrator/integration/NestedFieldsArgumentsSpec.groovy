package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class NestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {
    def mockAServiceResponse = [
            data: [
                    serviceA: []
            ]
    ]

    def mockBServiceResponse = [
            data: [
                    serviceB: []
            ]
    ]

    def "cannot Build Due To Query Nested Fields Has Mismatched Arguments"() {
        def serviceA = createSimpleMockService( "SVCA", "type Query { container : Container } "
                + "type Container { serviceA : ServiceA } "
                + "type ServiceA { svcAField1 : String }", mockAServiceResponse)

        def serviceB = createSimpleMockService( "SVCB", "type Query { container(in : String) : Container } "
                + "type Container { serviceB : ServiceB } "
                + "type ServiceB { svcBField1 : String }", mockBServiceResponse)


        when:
        ServiceProvider[] services = [ serviceA, serviceB ]
        GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        then:
        thrown(FieldMergeException)

        orchestrator == null
    }

}
