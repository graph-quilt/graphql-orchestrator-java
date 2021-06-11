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

public class UserRestService implements ServiceProvider {

  private final Map<String, Object> userDb;
  private final BiConsumer<ExecutionInput, GraphQLContext> assertFn;
  private static final BiConsumer NOOP = (_x, _y) -> {
  };

  public UserRestService() {
    this.userDb = createUsers();
    this.assertFn = NOOP;
  }

  public UserRestService(final BiConsumer<ExecutionInput, GraphQLContext> function) {
    this.userDb = createUsers();
    this.assertFn = function;
  }

  private Map<String, Object> createUsers() {

    Map<String, Object> p1 = new HashMap<>();
    p1.put("id", "user-1");
    p1.put("username", "delilah.hadfield");
    p1.put("password", "guesstheword");
    p1.put("firstName", "Delilah");
    p1.put("lastName", "Hadfield");
    p1.put("email", "delilah.hadfield@mail.com");
    p1.put("phone", "3436293059");
    p1.put("userStatus", UserStatus.ACTIVE);

    Map<String, Object> p2 = new HashMap<>();
    p2.put("id", "user-2");
    p2.put("username", "huong.seamon");
    p2.put("password", "idontknow");
    p2.put("firstName", "Huong");
    p2.put("lastName", "Seamon");
    p2.put("email", "huong.seamon@mail.com");
    p2.put("phone", "7676293059");
    p2.put("userStatus", UserStatus.DEACTIVATED);

    Map<String, Object> p3 = new HashMap<>();
    p3.put("id", "user-3");
    p3.put("username", "geraldine.gower");
    p3.put("password", "thenorthremembers");
    p3.put("firstName", "Geraldine");
    p3.put("lastName", "Gower");
    p3.put("email", "geraldine.gower@mail.com");
    p3.put("phone", "5856293059");
    p3.put("userStatus", UserStatus.PREACTIVE);

    Map<String, Object> newMap = new TreeMap<>();
    newMap.put("user-1", p1);
    newMap.put("user-2", p2);
    newMap.put("user-3", p3);
    return newMap;
  }

  @Override
  public ServiceType getSeviceType() {
    return ServiceType.REST;
  }

  @Override
  public String getNameSpace() {
    return "USER";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("top_level/user/user-schema.graphqls");
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
      if ("userById".equals(queryFieldName)) {
        Argument argument = field.getArguments().get(0);
        StringValue stringValue = (StringValue) argument.getValue();
        data.put(queryFieldName, this.userDb.get(stringValue.getValue()));
      }
      if ("users".equals(queryFieldName)) {
        data.put(queryFieldName, new ArrayList<>(userDb.values()));
      }
      if ("addUser".equals(queryFieldName)) {
        Map<String, Object> newuser = (Map<String, Object>) executionInput.getVariables().get("newuser");
        data.put(queryFieldName, newuser);
      }

    });
    return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
  }


  enum UserStatus {
    PREACTIVE,
    ACTIVE,
    DEACTIVATED
  }
}
