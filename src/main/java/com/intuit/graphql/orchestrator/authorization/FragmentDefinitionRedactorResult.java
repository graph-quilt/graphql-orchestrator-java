package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class FragmentDefinitionRedactorResult {

  @NonNull
  private FragmentDefinition fragmentDefinition;

  @NonNull
  private List<GraphqlErrorException> errors;
}
