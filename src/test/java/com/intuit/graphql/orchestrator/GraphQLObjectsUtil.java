package com.intuit.graphql.orchestrator;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.intuit.graphql.orchestrator.schema.Operation;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class GraphQLObjectsUtil {

  /**
   * Create an GraphQLObjectType with name Query and one {@link GraphQLFieldDefinition}  Intended for {@link Operation#QUERY}
   *
   * @param fieldName name of field
   * @param fieldType type of field
   * @return {@link GraphQLOutputType created
   */
  public static GraphQLObjectType createSimpleQueryType(String fieldName, GraphQLOutputType fieldType) {
    return GraphQLObjectType.newObject()
        .name("Query")
        .field(newFieldDefinition()
            .name(fieldName)
            .type(fieldType).build()).build();
  }

  /**
   *
   * Create an GraphQLObjectType with name Mutation and one {@link GraphQLFieldDefinition}  Intended for {@link Operation#MUTATION}
   *
   * @param fieldName name of field
   * @param fieldType type of field
   * @param fieldArgument argument definition for the field
   * @return {@link GraphQLOutputType created
   */
  public static GraphQLObjectType createSimpleMutationType(String fieldName, GraphQLOutputType fieldType,
    GraphQLArgument fieldArgument) {
    return GraphQLObjectType.newObject()
        .name("Mutation")
        .field(newFieldDefinition()
            .name(fieldName)
            .argument(fieldArgument)
            .type(fieldType).build()).build();
  }

  /**
   * Creates a field argument
   *
   * @param argName argument name
   * @param argType argument type
   * @return {@link GraphQLArgument created
   */
  public static GraphQLArgument createSimpleArgument(String argName, GraphQLInputType argType) {
    return GraphQLArgument.newArgument().name(argName).type(argType).build();
  }
}
