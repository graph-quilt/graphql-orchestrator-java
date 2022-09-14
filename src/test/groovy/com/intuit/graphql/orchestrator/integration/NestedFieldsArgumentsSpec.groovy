package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceA
import com.intuit.graphql.orchestrator.ServiceB
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class NestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {

    def "cannot Build Due To Query Nested Fields Has Mismatched Arguments"() {
        when:
        ServiceProvider[] services = [ new ServiceA(), new ServiceB() ]
        GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        then:
        thrown(FieldMergeException)

        orchestrator == null
    }

}
