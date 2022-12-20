package com.intuit.graphql.orchestrator;

import graphql.language.Field;
import graphql.language.SelectionSet;
import java.util.List;
import java.util.Optional;

public class SelectionSetUtil {

  public static Field getFieldByPath(List<String> pathList, SelectionSet selectionSet) {
    SelectionSet currentSelectionSet = selectionSet;

    Field currentField = null;
    for (String fieldName : pathList) {
      if (currentSelectionSet == null) return null;

      Optional<Field> optionalField =
          currentSelectionSet.getSelectionsOfType(Field.class).stream()
              .filter(field -> field.getName().equals(fieldName))
              .findFirst();

      if (!optionalField.isPresent()) return null;

      currentField = optionalField.get();
      currentSelectionSet = currentField.getSelectionSet();
    }

    return currentField;
  }
}
