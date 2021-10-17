package com.intuit.graphql.orchestrator.authorization;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;

/**
 * This interface provides a facility for applications using this library to implement field level
 * authorization.
 *
 * If the implementation of {@link #authorize(FieldCoordinates, Field, Object, Map)} requires
 * an input data, here referred to as authData, the {@link #getFutureAuthData()} may be implemented.
 * Otherwise, no need to implement {@link #getFutureAuthData()} as this interface provides a
 * default implementation which returns a completed future returning an empty string.
 *
 * graphQLContext.put(FieldAuthorization.class, appCustomFieldAuthorizationObject);
 */
public interface FieldAuthorization {

  /**
   * A hook to get an authData from CompletableFuture.
   *
   * @return future authorization data which shall be used as an input to
   * {@link #authorize(FieldCoordinates, Field, Object, Map)}
   */
  default CompletableFuture<Object> getFutureAuthData() {
    return CompletableFuture.completedFuture(StringUtils.EMPTY);
  }

  /**
   * interface for the authorization logic.
   *
   * @param fieldCoordinates field coordinate for the field
   * @param field the field that requires authorization
   * @param authData derived value from {@link #getFutureAuthData()}.  Empty string in case of
   *                 default implementation for {@link #getFutureAuthData()}.
   * @param argumentValues resolved argument values for the field
   * @return an instance of {@link FieldAuthorizationResult}
   */
  FieldAuthorizationResult authorize(FieldCoordinates fieldCoordinates, Field field,
      Object authData, Map<String, Object> argumentValues);
}
