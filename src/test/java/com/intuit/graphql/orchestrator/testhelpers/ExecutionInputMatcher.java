package com.intuit.graphql.orchestrator.testhelpers;

import graphql.ExecutionInput;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentMatcher;

@AllArgsConstructor
public class ExecutionInputMatcher implements ArgumentMatcher<ExecutionInput> {

  private ExecutionInput left;

  @Override
  public boolean matches(ExecutionInput right) {
    if (right != null) {
      return StringUtils.equals(left.getQuery(), right.getQuery());
    }
    return false;
  }
}
