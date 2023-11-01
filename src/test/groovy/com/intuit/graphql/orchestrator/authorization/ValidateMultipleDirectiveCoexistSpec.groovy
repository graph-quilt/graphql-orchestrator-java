package com.intuit.graphql.orchestrator.authorization

import com.intuit.graphql.orchestrator.stitching.InvalidDirectivePairingException
import spock.lang.Specification;
import com.intuit.graphql.graphQL.Directive

import com.intuit.graphql.graphQL.DirectiveDefinition
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirective
import static com.intuit.graphql.orchestrator.XtextObjectCreationUtil.buildDirectiveDefinition;

class ValidateMultipleDirectiveCoexistSpec extends Specification {
    private Directive resolverDirective

    private Directive providesDirective

    private Directive externalDirective

    private Directive requiresDirective

    private Directive skipDirective

    private Directive includesDirective



    def setup() {
        DirectiveDefinition resolverDirectiveDefinition = buildDirectiveDefinition("resolver")
        DirectiveDefinition externalDirectiveDefinition = buildDirectiveDefinition("external")
        DirectiveDefinition requiresDirectiveDefinition = buildDirectiveDefinition("requires")
        DirectiveDefinition providesDirectiveDefinition = buildDirectiveDefinition("provides")
        DirectiveDefinition skipDirectiveDefinition = buildDirectiveDefinition("skip")
        DirectiveDefinition includesDirectiveDefinition = buildDirectiveDefinition("include")
        resolverDirective = buildDirective(resolverDirectiveDefinition, Collections.emptyList())
        providesDirective = buildDirective(providesDirectiveDefinition, Collections.emptyList())
        externalDirective = buildDirective(externalDirectiveDefinition, Collections.emptyList())
        requiresDirective = buildDirective(requiresDirectiveDefinition, Collections.emptyList())
        skipDirective = buildDirective(skipDirectiveDefinition, Collections.emptyList())
        includesDirective = buildDirective(includesDirectiveDefinition, Collections.emptyList())
    }

    def "should throw exception for invalid directive pairing: resolver and external"() {
        given:
        def directives = [
                resolverDirective,
                externalDirective
        ]

        when:
        new ValidateMultipleDirectivesCoexist().validate(directives)

        then:
        thrown InvalidDirectivePairingException.class
    }

    def "should throw exception for invalid directive pairing: resolver and provides"() {
        given:
        def directives = [
                resolverDirective,
                providesDirective
        ]

        when:
        new ValidateMultipleDirectivesCoexist().validate(directives)

        then:
        thrown InvalidDirectivePairingException.class
    }

    def "should throw exception for invalid directive pairing: resolver and requires"() {
        given:
        def directives = [
                resolverDirective,
                requiresDirective
        ]

        when:
        new ValidateMultipleDirectivesCoexist().validate(directives)

        then:
        thrown InvalidDirectivePairingException.class
    }

    def "should not throw exception for valid directives"() {
        given:
        def directives = [
                requiresDirective,
                skipDirective,
                includesDirective
        ]

        when:
        new ValidateMultipleDirectivesCoexist().validate(directives)

        then:
        noExceptionThrown()
    }
}


