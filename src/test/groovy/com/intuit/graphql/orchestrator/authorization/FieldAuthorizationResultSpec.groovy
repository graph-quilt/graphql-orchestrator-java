package com.intuit.graphql.orchestrator.authorization

import graphql.GraphqlErrorException
import spock.lang.Specification

class FieldAuthorizationResultSpec extends Specification {

    GraphqlErrorException testGraphqlErrorException

    def setup() {

        def extensionsMap = [
                ext1: "value1",
                ext2: "value2"
        ]

        testGraphqlErrorException = GraphqlErrorException.newErrorException()
            .message("testMessage")
            .extensions(extensionsMap)
            .path(["a","b","c"])
            .build()
    }

    def "creates correct denied result"() {
        when:
        def actual = FieldAuthorizationResult.createDeniedResult(testGraphqlErrorException)

        then:
        actual.isAllowed() == false
        actual.getGraphqlErrorException() == testGraphqlErrorException
    }

}
