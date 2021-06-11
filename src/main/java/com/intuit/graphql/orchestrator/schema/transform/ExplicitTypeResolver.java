package com.intuit.graphql.orchestrator.schema.transform;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import java.util.Map;

public class ExplicitTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Map<String, Object> objData = env.getObject();
    if (objData.containsKey("__typename")) {
      return (GraphQLObjectType) env.getSchema().getType((String) objData.get("__typename"));
    } else {
      return null;
    }
  }
}
