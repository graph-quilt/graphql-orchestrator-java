package com.intuit.graphql.orchestrator.federation.metadata


import com.intuit.graphql.graphQL.Directive
import com.intuit.graphql.graphQL.DirectiveDefinition
import com.intuit.graphql.graphQL.ValueWithVariable
import graphql.language.Argument
import spock.lang.Specification

import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.*

class KeyDirectiveMetadataSpec extends Specification {
    def "KeyDirectiveMetadata from throws IllegalStateException for Directive with no arguments"() {
        given:
        Directive directive = generateKeyDirective("keyFieldName", false)
        when:
        KeyDirectiveMetadata.from(directive)
        then:
        thrown(IllegalStateException.class)
    }

    def "KeyDirectiveMetadata is returned by the from method for a Directive with arguments"() {
        given:
        Directive directive = generateKeyDirective("keyFieldName", true)
        KeyDirectiveMetadata keyDirectiveMetadata = KeyDirectiveMetadata.from(directive)

        expect:
        keyDirectiveMetadata != null

    }


    private Directive generateKeyDirective(String fieldSet, boolean addArgument) {
        ValueWithVariable fieldsInput = createValueWithVariable()
        fieldsInput.setStringValue(fieldSet)

        com.intuit.graphql.graphQL.Argument fieldsArgument = createArgument()
        fieldsArgument.setName("fields")
        fieldsArgument.setValueWithVariable(fieldsInput)

        DirectiveDefinition keyDefinition = createDirectiveDefinition()
        keyDefinition.setName("key")
        keyDefinition.setRepeatable(true)

        Directive keyDir = createDirective()
        keyDir.setDefinition(keyDefinition)
        if(addArgument) keyDir.getArguments().add(fieldsArgument)
        return keyDir
    }
}
