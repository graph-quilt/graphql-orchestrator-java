package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class CreateTypeExceptionSpec extends Specification {

    def "CreateTypeException is instance of RunTimeException and sets the correct error message"() {
        given:
        CreateTypeException createTypeException = new CreateTypeException("test Message")
        expect:
        createTypeException instanceof RuntimeException
        createTypeException.message == "test Message"
    }
}
