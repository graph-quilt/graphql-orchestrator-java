package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphQLContext;
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
  public void batchAuthorizeOrThrowGraphQLError(GraphQLContext context,
      List<DataFetchingEnvironment> keys, Object authData) throws GraphqlErrorException {
    // do nothing, allow access
  }

}
