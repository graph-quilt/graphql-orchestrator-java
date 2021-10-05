package com.intuit.graphql.orchestrator.authorization

import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class DefaultBatchFieldAuthorizationSpec extends Specification {

    def specUnderTest = new DefaultBatchFieldAuthorization()

    def "future auth data must be null"() {
        when:
        CompletableFuture<?> actual =  specUnderTest.getFutureAuthData()

        then:
        actual.get() == null
    }

}
