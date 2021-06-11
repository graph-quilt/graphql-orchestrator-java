package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class PetsService implements ServiceProvider {

  private Map<String, Object> petsDb;
  private final BiConsumer<ExecutionInput, GraphQLContext> assertFn;
  private static final BiConsumer NOOP = (_x, _y) -> {
  };

  public PetsService() {
    this.petsDb = createPets();
    this.assertFn = NOOP;
  }

  public PetsService(final BiConsumer<ExecutionInput, GraphQLContext> function) {
    this.petsDb = createPets();
    this.assertFn = function;
  }

  private Map<String, Object> createPets() {
    Map<String, Object> p1 = new HashMap<>();
    p1.put("id", "pet-1");
    p1.put("name", "Charlie");
    p1.put("age", 2);
    p1.put("weight", 200);
    p1.put("purebred", Boolean.TRUE);
    p1.put("type", "DOG");

    Map<String, Object> p2 = new HashMap<>();
    p2.put("id", "pet-2");
    p2.put("name", "Milo");
    p2.put("age", 1);
    p2.put("weight", 20);
    p2.put("purebred", Boolean.FALSE);
    p2.put("type", "RABBIT");

    Map<String, Object> p3 = new HashMap<>();
    p3.put("id", "pet-3");
    p3.put("name", "Poppy");
    p3.put("age", 5);
    p3.put("weight", 100);
    p3.put("purebred", Boolean.TRUE);
    p3.put("type", "CAT");

    Map<String, Object> newMap = new TreeMap<>();
    newMap.put("pet-1", p1);
    newMap.put("pet-2", p2);
    newMap.put("pet-3", p3);
    return newMap;
  }

  @Override
  public String getNameSpace() {
    return "PETS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("top_level/books-and-pets/schema-pets.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput, GraphQLContext context) {

    this.assertFn.accept(executionInput, context);

    Map<String, Object> data = new HashMap<>();

    Document document = (Document) executionInput.getRoot();
    OperationDefinition opDep = document.getDefinitionsOfType(OperationDefinition.class).get(0);
    opDep.getSelectionSet().getSelections().forEach(selection -> {
      Field field = (Field) selection;
      String queryFieldName = field.getName();
      if ("pet".equals(queryFieldName)) {
        Argument argument = field.getArguments().get(0);
        StringValue stringValue = (StringValue) argument.getValue();
        data.put(queryFieldName, this.petsDb.get(stringValue.getValue()));
      }
      if ("pets".equals(queryFieldName)) {
        data.put(queryFieldName, new ArrayList<>(petsDb.values()));
      }
    });
    return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
  }
}
