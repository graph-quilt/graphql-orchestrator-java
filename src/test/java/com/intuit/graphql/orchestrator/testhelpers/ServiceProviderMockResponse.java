package com.intuit.graphql.orchestrator.testhelpers;

import graphql.ExecutionInput;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ServiceProviderMockResponse {
  private String expectResponse;
  private ExecutionInput forExecutionInput;
}
