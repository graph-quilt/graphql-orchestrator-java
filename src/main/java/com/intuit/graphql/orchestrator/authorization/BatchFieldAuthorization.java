package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This interface provides a facility for users of this library to implement a field level
 * authorization in a batch manner, i.e. if one field is denied access for a given downstream
 * service, then no data will be returned for the service.
 */
public interface BatchFieldAuthorization {

  /**
   * Allows an authorization input data to be retrieved from a * CompletableFuture.
   *
   * @return future authorization data which shall be used an an input to {@link
   *     #batchAuthorizeOrThrowGraphQLError(Object, List)}
   */
  CompletableFuture<Object> getFutureAuthData();

  /**
   * This method shall implement the authorization logic.  Once the future authorization data is
   * available, it shall be used as an input to this method.
   *
   * @param authData {@link #getFutureAuthData()}
   * @param dataFetchingEnvironments DataFetchingEnvironments for a given a downstream service
   * @throws GraphqlErrorException if one of the field is denied access.
   */
  void batchAuthorizeOrThrowGraphQLError(
      Object authData, List<DataFetchingEnvironment> dataFetchingEnvironments)
      throws GraphqlErrorException;
}
