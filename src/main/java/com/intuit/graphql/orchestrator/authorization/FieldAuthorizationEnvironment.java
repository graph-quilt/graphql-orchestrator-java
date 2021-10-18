package com.intuit.graphql.orchestrator.authorization;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class FieldAuthorizationEnvironment {
  @NonNull private FieldCoordinates fieldCoordinates;
  @NonNull private Field field;
  @NonNull private Object authData;
  @NonNull private Map<String, Object> argumentValues;
  @NonNull private List<Object> path;
}
