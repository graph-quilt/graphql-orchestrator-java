package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultFieldAuthorization implements FieldAuthorization {

  public static final FieldAuthorizationResult ALLOWED_FIELD_AUTH_RESULT = new FieldAuthorizationResult(true);

  @Override
  public CompletableFuture<Object> getFutureAuthData() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void batchAuthorizeOrThrowGraphQLError(Object authData, List<DataFetchingEnvironment> keys)
      throws GraphqlErrorException {
    // do nothing, allow access
  }

  @Override
  public FieldAuthorizationResult authorize(FieldCoordinates fieldCoordinates, Field field, Object authData,
      Map<String, Object> argumentValues) {
    return ALLOWED_FIELD_AUTH_RESULT;
  }

}
