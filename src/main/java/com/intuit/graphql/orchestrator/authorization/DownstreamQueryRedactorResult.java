package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@Getter
@AllArgsConstructor
public class DownstreamQueryRedactorResult {

  private Node<?> node; // nullable since transformer may return null in case field representing this node is denied

  @NonNull
  private List<GraphqlErrorException> errors;

  boolean hasEmptySelectionSet;

  @NonNull
  private List<String> fragmentSpreadsRemoved;
}