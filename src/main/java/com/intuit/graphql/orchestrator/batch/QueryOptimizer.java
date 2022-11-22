package com.intuit.graphql.orchestrator.batch;

import graphql.language.Field;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class QueryOptimizer {

  private final Operation operationType;

  public QueryOptimizer(Operation operationType) {
    this.operationType = operationType;
  }

  public static ImmutablePair<List<Selection>, Map<String, List<Field>>> groupSelections(List<Selection> selections) {
    List<Selection> distinctSelections = new ArrayList<>();
    Map<String, List<Field>> fieldMap = new HashMap<>();
    selections.forEach(selection -> {
      //group fields with same name
      if (selection instanceof Field) {
        Field field = (Field) selection;
        fieldMap.computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(field);
        // collect non fields.
      } else {
        distinctSelections.add(selection);
      }
    });
    return new ImmutablePair<>(distinctSelections, fieldMap);
  }

  public SelectionSet getTransformedSelectionSet(SelectionSet selectionSet) {
    if (operationType == Operation.QUERY) {
      return recurse(selectionSet);
    }
    return selectionSet;
  }

  public SelectionSet recurse(SelectionSet selections) {
    SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();
    final ImmutablePair<List<Selection>, Map<String, List<Field>>> selectionSetTree = groupSelections(
        selections.getSelections());
    selectionSetTree.getLeft().forEach(selection -> mergedSelectionSetBuilder.selection(selection));
    selectionSetTree.getRight().values().forEach(fields -> mergedSelectionSetBuilder.selection(mergeFields(fields)));
    return mergedSelectionSetBuilder.build();
  }

  private Field mergeFields(final List<Field> fields) {
    final Field firstField = fields.get(0);
    // distinct field
    if (fields.size() == 1) {
      return fields.get(0);
    }
    // leaf node
    if (firstField.getSelectionSet() == null) {
      return firstField;
    }
    SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();
    fields.forEach(field ->
        field.getSelectionSet().getSelections().forEach(selection -> mergedSelectionSetBuilder.selection(selection))
    );
    return firstField.transform(builder ->
        builder.selectionSet(recurse(mergedSelectionSetBuilder.build()))
    );
  }

}
