package com.intuit.graphql.orchestrator.schema;

/**
 * Helper method that suppress unsafe casts for GraphQL Objects.
 */
public class GraphQLObjects {

  private GraphQLObjects() {

  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(Object o) {
    return (T) o;
  }
}
