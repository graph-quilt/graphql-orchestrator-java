package com.intuit.graphql.orchestrator;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StarWarsService implements ServiceProvider {

  StarWarsService() {
  }

  @Override
  public String getNameSpace() {
    return "STARWARS";
  }

  @Override
  public Map<String, String> sdlFiles() {
    return TestHelper.getFileMapFromList("top_level/starwars/schema-starwars.graphqls");
  }

  @Override
  public CompletableFuture<Map<String, Object>> query(ExecutionInput executionInput,
      GraphQLContext context) {

    Map<String, Object> data = new HashMap<>();

    Document document = (Document) executionInput.getRoot();
    OperationDefinition opDep = (OperationDefinition) document.getDefinitionsOfType(OperationDefinition.class).get(0);
    opDep.getSelectionSet().getSelections().stream().forEach(selection -> {
      Field field = (Field) selection;
      String queryFieldName = field.getName();
      switch (queryFieldName) {
        case "hero":
          data.put("hero", getHero());
          break;
        case "characters" :
          data.put("characters",Arrays.asList(getHuman(),getDroid()));
        case "human":
          data.put("human", getHuman());
        case "droid":
          data.put("droid", getDroid());
        default:
          break;
      }
    });
    return CompletableFuture.completedFuture(ImmutableMap.of("data", data));
  }

  private Map<String, Object> getHero() {
    Map<String, Object> r2d2 = new HashMap<>();
    Map<String, Object> c3po = new HashMap<>();
    r2d2.put("__typename", "Droid");
    r2d2.put("id", "c-1");
    r2d2.put("name", "R2-D2");
    r2d2.put("friends", Arrays.asList(c3po));
    r2d2.put("appearsIn", Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
    r2d2.put("primaryFunction", "Rescuing Luke");

    c3po.put("id", "c-1");
    c3po.put("name", "R2-D2");
    c3po.put("friends", Arrays.asList(r2d2));
    c3po.put("appearsIn", Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
    c3po.put("primaryFunction", "assist in etiquette, customs, and translation");
    return r2d2;
  }

  private Map<String, Object> getHuman() {
    Map<String, Object> obiwan = new HashMap<>();
    obiwan.put("__typename", "Human");
    obiwan.put("id", "c-2");
    obiwan.put("name", "Obi-Wan Kenobi");
    obiwan.put("friends", Collections.emptyList());
    obiwan.put("appearsIn", Arrays.asList("NEWHOPE", "EMPIRE"));
    obiwan.put("homePlanet", "Stewjon");
    return obiwan;
  }

  private Map<String, Object> getDroid() {
    Map<String, Object> r2d2 = new HashMap<>();
    r2d2.put("__typename", "Droid");
    r2d2.put("id", "c-1");
    r2d2.put("name", "R2-D2");
//    r2d2.put("friends", Arrays.asList(c3po));
    r2d2.put("appearsIn", Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
    r2d2.put("primaryFunction", "Rescuing Luke");
    return r2d2;
  }

}
