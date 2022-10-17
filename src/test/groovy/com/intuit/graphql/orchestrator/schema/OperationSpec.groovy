package com.intuit.graphql.orchestrator.schema

import spock.lang.Specification

class OperationSpec extends Specification {

    def "test Operation Name"() {
        expect:
        Operation.QUERY.getName() == "Query"
        Operation.MUTATION.getName() == "Mutation"
        Operation.SUBSCRIPTION.getName() == "Subscription"
    }

    def "test asGraphQLObjectType"() {
        expect:
        Operation.QUERY.asGraphQLObjectType().name == "Query"
        Operation.MUTATION.asGraphQLObjectType().name == "Mutation"
        Operation.SUBSCRIPTION.asGraphQLObjectType().name == "Subscription"
    }

}
