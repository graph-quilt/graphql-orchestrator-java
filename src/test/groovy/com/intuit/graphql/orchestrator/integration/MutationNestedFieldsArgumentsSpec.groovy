package com.intuit.graphql.orchestrator.integration

import com.intuit.graphql.orchestrator.GraphQLOrchestrator
import com.intuit.graphql.orchestrator.ServiceD
import com.intuit.graphql.orchestrator.ServiceE
import com.intuit.graphql.orchestrator.ServiceProvider
import com.intuit.graphql.orchestrator.schema.transform.FieldMergeException
import helpers.BaseIntegrationTestSpecification

class MutationNestedFieldsArgumentsSpec extends BaseIntegrationTestSpecification {

    def "cannot Build Due To Mutation Nested Fields Has Mismatched Arguments"() {
        given:
        ServiceProvider[] services = [ new ServiceD(), new ServiceE() ]

        when:
        GraphQLOrchestrator orchestrator = createGraphQLOrchestrator(services)

        then:
        thrown(FieldMergeException)

        orchestrator == null
    }
}
