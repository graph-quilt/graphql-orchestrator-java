package com.intuit.graphql.orchestrator.authorization;

import com.intuit.graphql.orchestrator.common.FieldPosition;
import graphql.GraphqlErrorException;
import java.util.concurrent.CompletableFuture;

public class DefaultFieldAuthorization<AuthDataT> implements FieldAuthorization<AuthDataT> {

  @Override
  public boolean isAccessAllowed(FieldAuthorizationEnvironment<AuthDataT> fieldAuthorizationEnvironment) {
    return true;
  }

  @Override
  public boolean requiresAccessControl(FieldPosition fieldPosition) {
    return false;
  }

  @Override
  public CompletableFuture<AuthDataT> getFutureAuthData() {
    throw new UnsupportedOperationException("getFutureAuthData should not be called when using DefaultFieldAuthorization.");
  }

  @Override
  public GraphqlErrorException getDeniedGraphQLErrorException() {
    throw new UnsupportedOperationException("getDeniedGraphQLErrorException should not be called when using DefaultFieldAuthorization.");
  }
}
