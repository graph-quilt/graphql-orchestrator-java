package com.intuit.graphql.orchestrator.batch

import graphql.ExecutionInput
import graphql.GraphQLContext
import spock.lang.Specification

class BatchLoaderExecutionHooksSpec extends Specification {

    void testDefaultMethods() {
        given:
        final BatchLoaderExecutionHooks<String, String> hooks = new BatchLoaderExecutionHooks<String, String>() {}

        when:
        GraphQLContext context = GraphQLContext.newContext().build();

        then:
        hooks.onBatchLoadEnd(context, Collections.singletonList(""));
        hooks.onBatchLoadStart(context, Collections.singletonList(""));
        hooks.onExecutionInput(context, Mock(ExecutionInput.class));
        hooks.onQueryResult(context, Collections.emptyMap());
    }

}
