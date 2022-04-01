package com.intuit.graphql.orchestrator.utils;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

/**
 * Collects set of fields from a selection set.  Fields collected may be from Inline Fragments
 * or Fragment Spreads.
 */
public class SelectionCollector {

  private final Map<String, FragmentDefinition> fragmentsByName = new HashMap<>();

  public SelectionCollector(Map<String, FragmentDefinition> fragmentsByName) {
    if (MapUtils.isNotEmpty(fragmentsByName)) {
      this.fragmentsByName.putAll(fragmentsByName);
    }
  }

  public Set<String> collectFields(SelectionSet selectionSet) {
    if (selectionSet == null || CollectionUtils.isEmpty(selectionSet.getSelections())) {
      return Collections.emptySet();
    }

    // TODO test that a selection set has been deduped if same field
    //  occurs in different selection (inline or fragment spread) on same level
    return selectionSet.getSelections().stream()
        .map(this::collectFields)
        .flatMap(Collection::stream)
        .map(Field::getName)
        .collect(Collectors.toSet());
  }

  private List<Field> collectFields(Selection selection) {
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

  private List<Field> getFields(FragmentSpread fragmentSpread) {
    Objects.requireNonNull(fragmentSpread);
    FragmentDefinition fragmentDefinition = fragmentsByName.get(fragmentSpread.getName());
    Objects.requireNonNull(fragmentDefinition);
    return getFields(fragmentDefinition.getSelectionSet().getSelections());
  }

  private List<Field> getFields(InlineFragment inlineFragment) {
    Objects.requireNonNull(inlineFragment);
    return getFields(inlineFragment.getSelectionSet().getSelections());
  }

  private List<Field> getFields(List<Selection> selections) {
    return selections.stream()
        .flatMap(selection -> collectFields(selection).stream())
        .collect(Collectors.toList());
  }

}
