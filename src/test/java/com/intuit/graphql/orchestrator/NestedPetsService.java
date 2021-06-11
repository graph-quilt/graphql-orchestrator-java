package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.util.Lists;

public class NestedPetsService implements ServiceProvider {

  private List<Pet> pets;

  NestedPetsService() {
    this.pets = createPets();
  }

  private List<Pet> createPets() {
    Pet p1 = new Pet("pet-1", "Charlie", 2, 200, Boolean.TRUE, "DOG");
    Pet p2 = new Pet("pet-2", "Milo", 2, 20, Boolean.FALSE, "RABBIT");
    Pet p3 = new Pet("pet-3", "Poppy", 5, 500, Boolean.TRUE, "CAT");
    return Lists.list(p1, p2, p3);
  }

  @Override
  public String getNameSpace() {
    return "PETS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("nested/books-pets-person/schema-pets.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    return CompletableFuture.completedFuture(
        ImmutableMap.of("data", ImmutableMap.of("person", ImmutableMap.of("pets", this.pets))));
  }

  @AllArgsConstructor
  @Getter
  @Setter
  private static class Pet {

    private String id;
    private String name;
    private Integer age;
    private Integer weight;
    private Boolean purebred;
    private String tag;
  }
}
