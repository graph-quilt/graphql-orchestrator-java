package com.intuit.graphql.orchestrator.batch;

import graphql.language.ArrayValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.Value;
import graphql.language.VariableReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VariableReferenceExtractor {

  private final Set<VariableReference> variableReferences = new HashSet<>();

  public Set<VariableReference> getVariableReferences() {
    return new HashSet<>(variableReferences);
  }

  public void captureVariableReferences(List<Value> values) {
    for (final Value value : values) {
      doSwitch(value);
    }
  }

  private void doSwitch(Value value) {
    if (value instanceof ArrayValue) {
      handleArrayValue((ArrayValue) value);
    } else if (value instanceof ObjectValue) {
      handleObjectValue(((ObjectValue) value));
    } else if (value instanceof VariableReference) {
      handleVariableReference((VariableReference) value);
    }
  }

  private void handleVariableReference(VariableReference variableReference) {
    variableReferences.add(variableReference);
  }

  private void handleArrayValue(ArrayValue arrayValue) {
    for (final Value value : arrayValue.getValues()) {
      doSwitch(value);
    }
  }

  private void handleObjectValue(ObjectValue objectValue) {
    for (final ObjectField objectField : objectValue.getObjectFields()) {
      doSwitch(objectField.getValue());
    }
  }
}
