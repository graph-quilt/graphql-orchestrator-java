package com.intuit.graphql.orchestrator.testhelpers;

import graphql.ExecutionResult;

public class CustomAssertions {

  public static ExecutionResultAssert assertThat(ExecutionResult actual) {
    return new ExecutionResultAssert(actual);
  }

}