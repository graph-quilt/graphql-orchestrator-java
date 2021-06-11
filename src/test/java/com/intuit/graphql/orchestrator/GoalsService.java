package com.intuit.graphql.orchestrator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.VariableReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GoalsService implements ServiceProvider {

  GoalsService() {
  }

  @Override
  public String getNameSpace() {
    return "GOALS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList(
        "top_level/goals-service/goals.graphqls",
        "top_level/goals-service/queries.graphqls",
        "top_level/goals-service/schema.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {

    Map<String, Object> data = new HashMap<>();

    Document document = (Document) executionInput.getRoot();
    OperationDefinition opDep = (OperationDefinition) document.getDefinitions().get(0);
    opDep.getSelectionSet().getSelections().stream().forEach(selection -> {
      Field field = (Field) selection;
      String queryFieldName = field.getName();
      if ("userGoals".equals(queryFieldName)) {
        data.put(queryFieldName, getUserGoals());
      }

      if ("userGoalImages".equals(queryFieldName)) {
        assertThat(field.getArguments().size()).isEqualTo(1);
        Argument argument = field.getArguments().get(0);
        VariableReference varRef = (VariableReference)argument.getValue();
        long longValue = (Long)executionInput.getVariables().get(varRef.getName());
        assertThat(longValue).isEqualTo(1);
        data.put(queryFieldName, Arrays.asList(ImmutableMap.of("imageUrl","SomeImageUrl","imageBlob","SomeImageBlob")));
      }
    });
    return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
  }

  private List<Map<String, Object>> getUserGoals() {
    List<Map<String, Object>> userGoals = new ArrayList<>();
    userGoals.add(createCarGoal());
    userGoals.add(createEducationalGoal());
    return userGoals;
  }

  private Map<String, Object> createCarGoal() {

    Map<String, Object> debtProvider = new HashMap<>();
    debtProvider.put("__typename","DebtProvider");
    debtProvider.put("id","dp-1");
    debtProvider.put("name","some debt provider");
    debtProvider.put("type","typa-A");
    debtProvider.put("currentValue",new BigDecimal(1.5));

    Map<String, Object> goal = new HashMap<>();
    goal.put("__typename","UserCarGoal");
    goal.put("id","goal-1");
    goal.put("goalType","AUTO"); // enum
    goal.put("name","Ultimate-Machine");
    goal.put("creationTime","2019-09-11T11:00:00-00:00");
    goal.put("images",  Arrays.asList(ImmutableMap.of("imageUrl","http://somefake.url.local/fakeimage.png")));
    goal.put("linkedProviders",Arrays.asList(debtProvider));

    goal.put("carMake","Jeep");
    return goal;
  }

  private Map<String, Object> createEducationalGoal() {

    Map<String, Object> debtProvider2 = new HashMap<>();
    debtProvider2.put("__typename","DebtProvider");
    debtProvider2.put("id","dp-2");
    debtProvider2.put("name","some debt provider 2");
    debtProvider2.put("type","typa-B");
    debtProvider2.put("currentValue",new BigDecimal(5.0));

    Map<String, Object> goal = new HashMap<>();
    goal.put("__typename","UserEducationGoal");
    goal.put("id","goal-2");
    goal.put("goalType","EDUCATION"); // enum
    goal.put("name","PhD-Goal");
    goal.put("creationTime","2019-01-12T11:02:02-02:05");
    goal.put("images",  Arrays.asList(ImmutableMap.of("imageUrl","http://anotherfake.url.local/fakeimage.png")));
    goal.put("linkedProviders",Arrays.asList(debtProvider2));

    goal.put("schoolCost","5000.00");
    return goal;
  }

}
