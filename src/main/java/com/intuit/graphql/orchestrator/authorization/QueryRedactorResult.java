package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.SelectionSetContainer;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class QueryRedactorResult {

  @NonNull
  private Node<?> node;

  private List<GraphqlErrorException> errors;
}
