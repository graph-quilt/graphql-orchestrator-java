package com.intuit.graphql.orchestrator.schema

import helpers.BaseIntegrationTestSpecification

class OperationSpec extends BaseIntegrationTestSpecification {

    def "test Operation Name"() {
        expect:
        Operation.QUERY.getName() == "Query"
        Operation.MUTATION.getName() == "Mutation"
        Operation.SUBSCRIPTION.getName() == "Subscription"
    }

}
