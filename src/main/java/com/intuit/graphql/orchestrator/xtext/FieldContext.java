package com.intuit.graphql.orchestrator.xtext;

import java.util.Objects;
import lombok.Getter;

@Getter
public class FieldContext {

  private final String parentType;
  private final String fieldName;

  public FieldContext(String parentType, String fieldName) {
    this.parentType = parentType;
    this.fieldName = fieldName;
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
