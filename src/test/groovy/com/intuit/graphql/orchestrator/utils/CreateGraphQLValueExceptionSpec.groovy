package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class CreateGraphQLValueExceptionSpec extends Specification {
    def "CreateGraphQLValueException is instance of RunTimeException and sets the correct error message"() {
        given:
        CreateGraphQLValueException createGraphQLValueException = new CreateGraphQLValueException("test Message")
        expect:
        createGraphQLValueException instanceof RuntimeException
        createGraphQLValueException.message == "test Message"
    }
}
