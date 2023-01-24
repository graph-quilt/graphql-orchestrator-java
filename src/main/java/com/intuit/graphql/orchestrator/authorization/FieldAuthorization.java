package com.intuit.graphql.orchestrator.authorization;

import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;

/**
 * This interface allows applications using this library to implement field level authorization. An
 * application may provide a custom implementation by adding it in the GraphqlQL Context e.g. *
 * graphQLContext.put(FieldAuthorization.class, appCustomFieldAuthorizationObject).
 *
 * <p>If the implementation of {@link #authorize(FieldAuthorizationEnvironment)} requires an input
 * data, here referred to as authData, the {@link #getFutureAuthData()} may be implemented.
 * Otherwise, no need to implement {@link #getFutureAuthData()} as this interface provides a default
 * implementation.
 */
public interface FieldAuthorization {

  /**
   * A hook to get an authData from CompletableFuture.
   *
   * @return future authorization data which shall be used as an input to
   * {@link #authorize(FieldAuthorizationEnvironment)}
   */
  default CompletableFuture<Object> getFutureAuthData() {
    return CompletableFuture.completedFuture(StringUtils.EMPTY);
  }

  /**
   * interface for the authorization logic.
   *
   * @param fieldAuthorizationEnvironment data about field requiring authorization
   * @return an instance of {@link FieldAuthorizationResult}
   */
  FieldAuthorizationResult authorize(FieldAuthorizationEnvironment fieldAuthorizationEnvironment);
}