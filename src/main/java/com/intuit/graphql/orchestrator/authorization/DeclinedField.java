package com.intuit.graphql.orchestrator.authorization;

import graphql.language.Field;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class DeclinedField {

  @NonNull
  private Field field;

  @NonNull
  private List<String> pathList;

}
