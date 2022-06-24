package com.intuit.graphql.orchestrator.resolverdirective

import com.intuit.graphql.orchestrator.TestHelper
import graphql.Scalars
import graphql.schema.GraphQLArgument
import helpers.BaseIntegrationTestSpecification

class ResolverArgumentDirectiveSpec extends BaseIntegrationTestSpecification {

    void createsFromGraphQLArgument() {
        given:
        String schema = '''
            schema { query: Query }
            type Query { a(arg: Int @resolver(field: "a.b.c")): Int } 
            directive @resolver(field: String) on ARGUMENT_DEFINITION
        '''
        final GraphQLArgument arg = TestHelper.schema(schema).getQueryType().getFieldDefinition("a")
                .getArgument("arg")

        when:
        final ResolverArgumentDirective result = ResolverArgumentDirective.fromGraphQLArgument(arg)

        then:
        result.getArgumentName() == "arg"
        result.getField() == "a.b.c"
        result.getInputType() == Scalars.GraphQLInt
    }

}
