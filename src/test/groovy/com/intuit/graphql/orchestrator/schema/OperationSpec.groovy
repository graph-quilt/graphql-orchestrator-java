package com.intuit.graphql.orchestrator.schema

import spock.lang.Specification

class OperationSpec extends Specification {

    def "test Operation Name"() {
        expect:
        Operation.QUERY.getName() == "Query"
        Operation.MUTATION.getName() == "Mutation"
        Operation.SUBSCRIPTION.getName() == "Subscription"
    }

}
