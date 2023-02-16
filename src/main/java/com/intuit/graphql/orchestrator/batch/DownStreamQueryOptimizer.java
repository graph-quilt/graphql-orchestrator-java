package com.intuit.graphql.orchestrator.batch;

import static com.intuit.graphql.orchestrator.utils.SelectionSetUtil.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import graphql.language.Field;
import graphql.language.OperationDefinition.Operation;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;

public class DownStreamQueryOptimizer {

  private final Operation operationType;

  public DownStreamQueryOptimizer(Operation operationType) {
    this.operationType = operationType;
  }

  public SelectionSet getTransformedSelectionSet(SelectionSet selectionSet) {
    if (operationType == Operation.QUERY) {
      return transform(selectionSet);
    }
    return selectionSet;
  }
  private static GroupedSelectionSet groupSelections(List<Selection> selections) {
    GroupedSelectionSet groupedSelectionSet = new GroupedSelectionSet();
    selections.forEach(selection -> {
      //group fields with same name

      if (selection instanceof Field) {
        Field field = (Field) selection;
        // do not merge if field has arguments
        if (isNotEmpty(field.getArguments()) || isNotEmpty(field.getDirectives())) {
          groupedSelectionSet.getDistinctSelections().add(selection);
        } else {
          groupedSelectionSet.getGroupedFields().computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(field);
        }
        // collect non fields for now.
        // ToDo: Will we have a case where we have to merge fragments ?
      } else {
        groupedSelectionSet.getDistinctSelections().add(selection);
      }
    });
    return groupedSelectionSet;
  }

  private SelectionSet transform(SelectionSet selections) {
    final GroupedSelectionSet groupedSelectionSet = groupSelections(selections.getSelections());
    if(groupedSelectionSet.getGroupedFields().size() > 0) {
      SelectionSet.Builder mergedSelectionSetBuilder = SelectionSet.newSelectionSet();
      groupedSelectionSet.getDistinctSelections().forEach(selection -> mergedSelectionSetBuilder.selection(selection));
      groupedSelectionSet.getGroupedFields().values()
          .forEach(fields -> mergedSelectionSetBuilder.selection(mergeFields(fields)));
      return mergedSelectionSetBuilder.build();
    }
    return selections;
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
