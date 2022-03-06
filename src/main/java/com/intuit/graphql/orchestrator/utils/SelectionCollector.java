package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SelectionCollector {

  private final Map<String, FragmentDefinition> fragmentsByName;

  public SelectionCollector(Map<String, FragmentDefinition> fragmentsByName) {
    this.fragmentsByName = fragmentsByName;
  }

  public List<Field> collectFields(Selection selection) {
    if (selection instanceof Field) {
      return Collections.singletonList((Field) selection);
    } else if (selection instanceof FragmentSpread) {
      return getFields((FragmentSpread) selection);
    } else if (selection instanceof InlineFragment) {
      return getFields((InlineFragment) selection);
    } else {
      // the application is not in correct state.  Might consider updating.
      throw new IllegalStateException(
          "Unexpected selection instance.  class=" + selection.getClass().getName());
    }
  }

  public List<Field> getFields(FragmentSpread fragmentSpread) {
    Objects.requireNonNull(fragmentSpread);
    FragmentDefinition fragmentDefinition = fragmentsByName.get(fragmentSpread.getName());
    Objects.requireNonNull(fragmentDefinition);
    return getFields(fragmentDefinition.getSelectionSet().getSelections());
  }

  public List<Field> getFields(InlineFragment inlineFragment) {
    Objects.requireNonNull(inlineFragment);
    return getFields(inlineFragment.getSelectionSet().getSelections());
  }

  public List<Field> getFields(List<Selection> selections) {
    return selections.stream()
        .flatMap(selection -> collectFields(selection).stream())
        .collect(Collectors.toList());
  }
}
