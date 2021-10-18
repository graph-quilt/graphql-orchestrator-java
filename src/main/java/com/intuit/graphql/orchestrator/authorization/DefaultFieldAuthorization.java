package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.authorization.FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT;

public class DefaultFieldAuthorization implements FieldAuthorization {

  @Override
  public FieldAuthorizationResult authorize(FieldAuthorizationEnvironment fieldAuthorizationEnvironment) {
    return ALLOWED_FIELD_AUTH_RESULT;
  }
}
