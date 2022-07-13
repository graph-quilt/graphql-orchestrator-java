package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.xtext.FieldContext
import spock.lang.Specification

class ResolverArgumentFieldRootObjectDoesNotExistSpec extends Specification {

    def "produces Correct Error Message"() {
        given:
        final ResolverArgumentFieldRootObjectDoesNotExist error = new ResolverArgumentFieldRootObjectDoesNotExist(
                "argName", new FieldContext("rootObject", "rootField"), "tax")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': field 'tax' does not exist in schema.")
    }
}
