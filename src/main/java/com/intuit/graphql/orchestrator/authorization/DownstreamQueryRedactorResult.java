package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Node;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class DownstreamQueryRedactorResult {

  @NonNull
  private Node<?> node;

  @NonNull
  private List<GraphqlErrorException> errors;
}