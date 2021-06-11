package com.intuit.graphql.orchestrator.batch;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@SuppressWarnings("rawtypes")
public class MergedFieldModifier {


  private final DataFetchingEnvironment env;

  private final Map<String, FragmentDefinition> modifiedFragmentDefinitions = new HashMap<>();

  public MergedFieldModifier(final DataFetchingEnvironment env) {
    this.env = Objects.requireNonNull(env);
  }

  public MergedFieldModifierResult getFilteredRootField() {

    List<Field> validFields = new ArrayList<>();
    String expectedPath = env.getExecutionStepInfo().getPath().toString();

    for (final Field field : getRootField(env.getExecutionStepInfo()).getFields()) {

      Selection f = filterIrrelevantFields(field, "", expectedPath);

      if (f != null) {
        validFields.add(((Field) f));
      }
    }

    if (validFields.isEmpty()) {
      return null;
    }

    return new MergedFieldModifierResult(env.getMergedField().transform(builder -> builder.fields(validFields)),
        modifiedFragmentDefinitions);
  }

  private Selection filterIrrelevantFields(Selection field, String acc, String expectedPath) {
    if (field instanceof Field) {
      Field fieldToCheck = (Field) field;

      acc += "/" + (fieldToCheck.getAlias() == null ? fieldToCheck.getName() : fieldToCheck.getAlias());

      if (!expectedPath.startsWith(acc)) {
        return null;
      } else if (expectedPath.equals(acc)) {
        return fieldToCheck;
      }

      SelectionSet filteredSelectionSet = filterSelectionSet(fieldToCheck.getSelectionSet(), acc, expectedPath);

      return filteredSelectionSet != null ?
          fieldToCheck.transform(builder -> builder.selectionSet(filteredSelectionSet)) : null;

    } else if (field instanceof FragmentSpread) {
      FragmentDefinition fragmentDefinition = env.getFragmentsByName().get(((FragmentSpread) field).getName());
      SelectionSet filteredSelectionSet = filterSelectionSet(fragmentDefinition.getSelectionSet(), acc, expectedPath);

      if (filteredSelectionSet == null) {
        return null;
      }

      FragmentDefinition newFragmentDefinition = fragmentDefinition
          .transform(builder -> builder.selectionSet(filteredSelectionSet));

      modifiedFragmentDefinitions.put(newFragmentDefinition.getName(), newFragmentDefinition);

      return field;
    } else {
      //inline fragment
      final InlineFragment inlineFragment = (InlineFragment) field;
      SelectionSet filteredSelectionSet = filterSelectionSet(inlineFragment.getSelectionSet(), acc, expectedPath);

      return filteredSelectionSet != null ? inlineFragment
          .transform(builder -> builder.selectionSet(filteredSelectionSet)) : null;
    }
  }

  private SelectionSet filterSelectionSet(SelectionSet selectionSet, String acc, String expectedPath) {
    if (selectionSet == null) {
      return null;
    }

    List<Selection> filteredSelections = new ArrayList<>();

    if (selectionSet.getSelections() == null) {
      return selectionSet;
    }

    for (final Selection selection : selectionSet.getSelections()) {
      Selection filteredSelection = filterIrrelevantFields(selection, acc, expectedPath);

      if (filteredSelection != null) {
        filteredSelections.add(filteredSelection);
      }
    }

    if (filteredSelections.isEmpty()) {
      return null;
    }

    return selectionSet.transform(builder -> builder.selections(filteredSelections));
  }

  private MergedField getRootField(ExecutionStepInfo executionStepInfo) {
    ExecutionStepInfo curr = executionStepInfo;

    while (curr.getPath().getLevel() != 1) {
      curr = curr.getParent();
    }

    return curr.getField();
  }

  @Getter
  static class MergedFieldModifierResult {

    private final MergedField mergedField;
    private final Map<String, FragmentDefinition> fragmentDefinitions;

    private MergedFieldModifierResult(final MergedField mergedField,
        final Map<String, FragmentDefinition> fragmentDefinitions) {
      this.mergedField = mergedField;
      this.fragmentDefinitions = fragmentDefinitions;
    }
  }
}