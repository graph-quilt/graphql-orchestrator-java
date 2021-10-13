package com.intuit.graphql.orchestrator.authorization;

import graphql.GraphqlErrorException;
import graphql.language.Field;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class FieldRedactorResult {

  @NonNull
  private Field field;

  @NonNull
  private List<GraphqlErrorException> errors;
}
