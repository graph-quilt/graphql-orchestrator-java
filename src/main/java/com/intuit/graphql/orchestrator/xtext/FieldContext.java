package com.intuit.graphql.orchestrator.xtext;

import graphql.schema.FieldCoordinates;
import java.util.Objects;
import lombok.Getter;

@Getter
public class FieldContext {

  private final String parentType;
  private final String fieldName;
  private final FieldCoordinates fieldCoordinates;

  public FieldContext(String parentType, String fieldName) {
    this.parentType = parentType;
    this.fieldName = fieldName;
    this.fieldCoordinates = FieldCoordinates.coordinates(parentType, fieldName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldContext that = (FieldContext) o;
    return Objects.equals(parentType, that.parentType) &&
        Objects.equals(fieldName, that.fieldName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentType, fieldName);
  }

  @Override
  public String toString() {
    return parentType + ":" + fieldName;
  }
}
