package com.intuit.graphql.orchestrator.schema

import spock.lang.Specification

class SchemaParseExceptionSpec extends Specification {

    def "Exception message is correctly formed for exception with msg argument"() {
        given:
        SchemaParseException schemaParseException = new SchemaParseException("test Message")
        expect:
        schemaParseException.message == "test Message"
    }

    def "Exception message is correctly formed for exception with msg and throwable argument"() {
        given:
        SchemaParseException schemaParseException = new SchemaParseException("test Message", new Throwable())
        expect:
        schemaParseException.message == "test Message"
        schemaParseException.cause != null
    }
}
