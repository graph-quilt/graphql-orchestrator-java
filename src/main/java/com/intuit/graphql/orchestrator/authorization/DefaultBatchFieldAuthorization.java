package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DefaultBatchFieldAuthorization implements BatchFieldAuthorization {

  @Override
  public CompletableFuture<Object> getFutureAuthData() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void batchAuthorizeOrThrowGraphQLError(Object authData, List<DataFetchingEnvironment> keys)
      throws GraphqlErrorException {
    // do nothing, allow access
  }

}
