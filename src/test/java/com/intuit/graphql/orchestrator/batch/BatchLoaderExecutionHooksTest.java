package com.intuit.graphql.orchestrator.batch;

import static org.mockito.Mockito.mock;

import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.Collections;
import org.junit.Test;

public class BatchLoaderExecutionHooksTest {

  @Test
  public void testDefaultMethods() {
    final BatchLoaderExecutionHooks<String, String> hooks = new BatchLoaderExecutionHooks<String, String>() {

    };

    GraphQLContext context = GraphQLContext.newContext().build();

    hooks.onBatchLoadEnd(context, Collections.singletonList(""));
    hooks.onBatchLoadStart(context, Collections.singletonList(""));
    hooks.onExecutionInput(context, mock(ExecutionInput.class));
    hooks.onQueryResult(context, Collections.emptyMap());
  }
}