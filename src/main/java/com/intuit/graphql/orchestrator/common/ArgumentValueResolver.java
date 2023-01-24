package com.intuit.graphql.orchestrator.common;

import graphql.execution.ValuesResolver;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ArgumentValueResolver {

  public Map<String, Object> resolve(GraphQLSchema graphQLSchema, GraphQLFieldDefinition fieldDefinition,
      Field field, Map<String, Object> queryVariables) {

    ValuesResolver valuesResolver = new ValuesResolver();
    return valuesResolver.getArgumentValues(graphQLSchema.getCodeRegistry(), fieldDefinition.getArguments(),
        field.getArguments(), queryVariables);
  }
}