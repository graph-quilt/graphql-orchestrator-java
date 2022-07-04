package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.xtext.FieldContext
import spock.lang.Specification

class ResolverArgumentFieldNotInSchemaSpec extends Specification {

    void producesCorrectErrorMessage() {
        given:
        final ResolverArgumentFieldNotInSchema error = new ResolverArgumentFieldNotInSchema(
                "argName", new FieldContext("rootObject", "rootField"),
                new FieldContext("parentObject", "parentField"))

        expect:
        error.message.contains(
                "Resolver argument 'argName' in 'rootObject:rootField': field 'parentField' in InputType 'parentObject' does not exist in schema.")
    }
}
