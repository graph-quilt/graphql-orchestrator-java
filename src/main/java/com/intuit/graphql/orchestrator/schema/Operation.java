package com.intuit.graphql.orchestrator.schema;


import static com.intuit.graphql.orchestrator.xtext.GraphQLFactoryDelegate.createObjectTypeDefinition;

import com.intuit.graphql.graphQL.ObjectTypeDefinition;
import graphql.schema.GraphQLObjectType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * The enum Operation.
 */
public enum Operation {


  QUERY("Query"),
  MUTATION("Mutation"),
  SUBSCRIPTION("Subscription");

  private final String name;

  Operation(String name) {
    this.name = name;
  }

  private static final Map<String, Operation> operationMap = new HashMap<>();

  static {
    EnumSet.allOf(Operation.class).forEach(operation -> {
      operationMap.put(StringUtils.lowerCase(operation.getName()), operation);
    });
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * converts operations to an GraphQLObjectType.
   *
   * @return the GraphQLObjectType
   */
  public GraphQLObjectType asGraphQLObjectType() {
    return GraphQLObjectType.newObject().name(this.name).build();
  }

  /**
   * converts operations to an ObjectTypeDefinition.
   *
   * @return the ObjectTypeDefinition
   */
  public ObjectTypeDefinition asObjectTypeDefinition() {
    ObjectTypeDefinition objectTypeDefinition = createObjectTypeDefinition();
    objectTypeDefinition.setName(name);
    return objectTypeDefinition;
  }

}
