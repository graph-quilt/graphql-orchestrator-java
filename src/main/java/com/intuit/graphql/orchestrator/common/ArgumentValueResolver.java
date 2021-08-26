package com.intuit.graphql.orchestrator.common;

import graphql.execution.ValuesResolver;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import lombok.AllArgsConstructor;
import java.util.Map;

@AllArgsConstructor
public class ArgumentValueResolver {

  protected final ValuesResolver valuesResolver = new ValuesResolver();
  protected final GraphQLSchema graphQLSchema;

  public Map<String, Object> resolve(Field field, GraphQLFieldDefinition fieldDefinition, Map<String, Object> variables) {
    return valuesResolver.getArgumentValues(graphQLSchema.getCodeRegistry(), fieldDefinition.getArguments(),
        field.getArguments(), variables
    );
  }
}
