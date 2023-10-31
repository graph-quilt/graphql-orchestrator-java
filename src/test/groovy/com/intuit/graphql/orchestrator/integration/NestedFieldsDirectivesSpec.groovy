package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class NestedFieldsDirectivesSpec extends BaseIntegrationTestSpecification {

    def mockAServiceResponse = [
            data: [
                    serviceA: []
            ]
    ]

    def mockCServiceResponse = [
            data: [
                    serviceC: []
            ]
    ]


    def "cannot Build Due To Query Nested Fields Has Mismatched Directives"() {
        def serviceA = createSimpleMockService( "SVCA", "type Query { container : Container } "
                + "type Container { serviceA : ServiceA } "
                + "type ServiceA { svcAField1 : String }", mockAServiceResponse)

        def serviceC = createSimpleMockService( "SVCC", "type Query { container : Container @deprecated } "
                + "type Container { serviceC : ServiceC } "
                + "type ServiceC { svcCField1 : String }", mockCServiceResponse)


        when:
        ServiceProvider[] services = [serviceA, serviceC]
        GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        then:
        thrown(FieldMergeException)

        orchestrator == null
    }

}
