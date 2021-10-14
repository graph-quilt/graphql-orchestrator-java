package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import lombok.Getter;

@Getter
public class FieldAuthorizationResult {

  private boolean isAllowed;
  private GraphqlErrorException graphqlErrorException;

  public FieldAuthorizationResult(boolean isAllowed) {
    this.isAllowed = isAllowed;
  }

  public FieldAuthorizationResult(boolean isAllowed, GraphqlErrorException graphqlErrorException) {
    this.isAllowed = isAllowed;
    this.graphqlErrorException = graphqlErrorException;
  }

}
