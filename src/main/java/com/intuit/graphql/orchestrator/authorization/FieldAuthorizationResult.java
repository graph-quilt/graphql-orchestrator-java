package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import java.util.Objects;
import lombok.Getter;

public class FieldAuthorizationResult {

  public static final FieldAuthorizationResult ALLOWED_FIELD_AUTH_RESULT =
      new FieldAuthorizationResult(true, null);

  @Getter private final boolean isAllowed;
  @Getter private final GraphqlErrorException graphqlErrorException;

  private FieldAuthorizationResult(boolean isAllowed, GraphqlErrorException graphqlErrorException) {
    this.isAllowed = isAllowed;
    this.graphqlErrorException = graphqlErrorException;
  }

  public static FieldAuthorizationResult createDeniedResult(GraphqlErrorException graphqlErrorException) {
    Objects.requireNonNull(graphqlErrorException, "an instance of GraphqlErrorException is "
        + "required to create a denied FieldAuthorizationResult");
    return new FieldAuthorizationResult(false, graphqlErrorException);
  }

}
