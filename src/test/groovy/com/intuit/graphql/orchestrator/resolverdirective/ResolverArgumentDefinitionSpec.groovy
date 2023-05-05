package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.graphQL.NamedType
import com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate
import spock.lang.Specification

class ResolverArgumentDefinitionSpec extends Specification {
    private String TEST_NAME = "testName"
    private String TEST_VALUE = "testValue"

    def "Builder with no Params is accessible"() {
        given:
        ResolverArgumentDefinition.Builder builder = new ResolverArgumentDefinition.Builder()
        expect:
        builder instanceof ResolverArgumentDefinition.Builder
    }

    def "Builder with arguments sets the values correctly"() {
        given:
        NamedType namedType = GraphQLFactoryDelegate.createObjectType()
        ResolverArgumentDefinition resolverArgumentDefinition = new ResolverArgumentDefinition.Builder()
                .name(TEST_NAME)
                .value(TEST_VALUE)
                .namedType(namedType)
                .build()

        expect:
        resolverArgumentDefinition.name == TEST_NAME
        resolverArgumentDefinition.value == TEST_VALUE
        resolverArgumentDefinition.namedType == namedType
    }
}
