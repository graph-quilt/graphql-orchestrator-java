package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.utils.SelectionSetUtil.isEmpty;

import com.intuit.graphql.orchestrator.utils.SelectionSetUtil;
import graphql.language.Field;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

public class QueryOptimizer {

  private final Operation operationType;

  public QueryOptimizer(Operation operationType) {
    this.operationType = operationType;
  }

  private static GroupedSelectionSet groupSelections(List<Selection> selections) {
    GroupedSelectionSet groupedSelectionSet = new GroupedSelectionSet();
    selections.forEach(selection -> {
      //group fields with same name
      if (selection instanceof Field) {
        Field field = (Field) selection;
        groupedSelectionSet.getGroupedFields().computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(field);
        // collect non fields.
      } else {
        groupedSelectionSet.getDistinctSelections().add(selection);
      }
    });
    return groupedSelectionSet;
  }

  public SelectionSet getTransformedSelectionSet(SelectionSet selectionSet) {
    if (operationType == Operation.QUERY) {
      return transform(selectionSet);
    }
    return selectionSet;
  }

  private SelectionSet transform(SelectionSet selections) {
    SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();
    final GroupedSelectionSet groupedSelectionSet = groupSelections(selections.getSelections());
    groupedSelectionSet.getDistinctSelections().forEach(selection -> mergedSelectionSetBuilder.selection(selection));
    groupedSelectionSet.getGroupedFields().values()
        .forEach(fields -> mergedSelectionSetBuilder.selection(mergeFields(fields)));
    return mergedSelectionSetBuilder.build();
  }

  private Field mergeFields(final List<Field> fields) {
    final Field firstField = fields.get(0);
    // distinct field
    if (fields.size() == 1) {
      return firstField;
    }
    // leaf node
    if (isEmpty(firstField.getSelectionSet())) {
      return firstField;
    }
    SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();
    fields.forEach(field ->
        field.getSelectionSet().getSelections().forEach(selection -> mergedSelectionSetBuilder.selection(selection))
    );
    return firstField.transform(builder ->
        builder.selectionSet(transform(mergedSelectionSetBuilder.build()))
    );
  }

  @Getter
  static class GroupedSelectionSet {

    List<Selection> distinctSelections = new ArrayList<>();
    Map<String, List<Field>> groupedFields = new HashMap<>();
  }

}
