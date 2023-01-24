package com.intuit.graphql.orchestrator.authorization;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
@EqualsAndHashCode
public class FieldAuthorizationEnvironment {
  @NonNull @EqualsAndHashCode.Include
  private FieldCoordinates fieldCoordinates;
  @NonNull @EqualsAndHashCode.Exclude
  private Field field;
  @NonNull @EqualsAndHashCode.Exclude
  private Object authData;
  @NonNull @EqualsAndHashCode.Exclude
  private Map<String, Object> argumentValues;
  @NonNull @EqualsAndHashCode.Exclude
  private List<Object> path;
}