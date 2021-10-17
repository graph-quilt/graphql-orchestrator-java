package com.intuit.graphql.orchestrator.authorization;

import static com.intuit.graphql.orchestrator.authorization.FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.Map;

public class DefaultFieldAuthorization implements FieldAuthorization {

  @Override
  public FieldAuthorizationResult authorize(FieldCoordinates fieldCoordinates, Field field, Object authData,
      Map<String, Object> argumentValues) {
    return ALLOWED_FIELD_AUTH_RESULT;
  }

}
