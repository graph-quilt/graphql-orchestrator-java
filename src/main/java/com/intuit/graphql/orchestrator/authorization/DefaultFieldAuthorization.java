package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.authorization.FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT;

/**
 * This is the class used as default implementation of {@link FieldAuthorization}
 * if no custom implementation is provided.  see {@link FieldAuthorization} for more information.
 */
public class DefaultFieldAuthorization implements FieldAuthorization {

  @Override
  public FieldAuthorizationResult authorize(FieldAuthorizationEnvironment fieldAuthorizationEnvironment) {
    return ALLOWED_FIELD_AUTH_RESULT;
  }

}