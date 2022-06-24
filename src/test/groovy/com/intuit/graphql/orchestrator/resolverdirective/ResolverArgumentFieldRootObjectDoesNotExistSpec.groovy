package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.xtext.FieldContext
import helpers.BaseIntegrationTestSpecification

class ResolverArgumentFieldRootObjectDoesNotExistSpec extends BaseIntegrationTestSpecification {

    void producesCorrectErrorMessage() {
        given:
        final ResolverArgumentFieldRootObjectDoesNotExist error = new ResolverArgumentFieldRootObjectDoesNotExist(
                "argName", new FieldContext("rootObject", "rootField"), "tax")

        expect:
        error.message.contains("Resolver argument 'argName' in 'rootObject:rootField': field 'tax' does not exist in schema.")
    }
}
