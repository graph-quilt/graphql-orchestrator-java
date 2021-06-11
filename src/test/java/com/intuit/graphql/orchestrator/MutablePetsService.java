package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MutablePetsService implements ServiceProvider {

  @Override
  public String getNameSpace() {
    return "PETS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("top_level/books-and-pets/schema-pets.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {

    Document document = (Document) executionInput.getRoot();
    OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0);
    Field field = (Field) opDep.getSelectionSet().getSelections().get(0);
    String queryFieldName = field.getName();

    Map<String, Object> newpet = (Map<String, Object>) executionInput.getVariables().get("newpet");
    return CompletableFuture
        .completedFuture(ImmutableMap.of("data", ImmutableMap.of(queryFieldName, newpet)));
  }
}
