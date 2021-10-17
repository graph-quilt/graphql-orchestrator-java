package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import java.util.Objects;
import lombok.Getter;

public class FieldAuthorizationResult {

  public static final FieldAuthorizationResult ALLOWED_FIELD_AUTH_RESULT = new FieldAuthorizationResult();

  @Getter private final boolean isAllowed;
  @Getter private final GraphqlErrorException graphqlErrorException;

  private FieldAuthorizationResult() {
    this.isAllowed = true;
    graphqlErrorException = null;
  }

  private FieldAuthorizationResult(GraphqlErrorException graphqlErrorException) {
    this.isAllowed = false;
    this.graphqlErrorException = graphqlErrorException;
  }

  public static FieldAuthorizationResult createDeniedResult(GraphqlErrorException graphqlErrorException) {
    Objects.requireNonNull(graphqlErrorException, "an instance of GraphqlErrorException is "
        + "required to create a denied FieldAuthorizationResult");
    return new FieldAuthorizationResult(graphqlErrorException);
  }

}
