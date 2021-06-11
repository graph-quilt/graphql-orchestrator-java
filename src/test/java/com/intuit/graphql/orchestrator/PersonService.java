package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PersonService implements ServiceProvider {

  private Map<String, Object> person;

  PersonService() {
    this.person = createPerson();
  }

  private Map<String, Object> createPerson() {
    Map<String, Object> addressMap = new HashMap<>();
    addressMap.put("id","address-1");
    addressMap.put("street", "Lombok Street");
    addressMap.put("city", "San Diego");
    addressMap.put("zip", "12345");
    addressMap.put("state", "CA");
    addressMap.put("country", "United States");
    return ImmutableMap.of("id", "person-1", "name", "Kevin Whitney", "address", addressMap);
  }

  @Override
  public String getNameSpace() {
    return "PERSON";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("nested/books-pets-person/schema-person.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {
    return CompletableFuture.completedFuture(ImmutableMap.of("data", ImmutableMap.of("person", this.person)));
  }
}
