package com.intuit.graphql.orchestrator.keydirective;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLInputType;
import lombok.Getter;

import java.util.Objects;

/**
 * This class holds information about the key directive without the need to parse it via Xtext or
 * GraphQL-Java.
 */
@Getter
public class KeyDirective {

  /**
   * The field in the key directive {@code @key(fields: '')}
   */
  private final String fields;

  private KeyDirective(final Builder builder) {
    fields = builder.fields;
  }

  public static KeyDirective fromGraphQLArgument(GraphQLArgument graphQLArgument) {
    Builder b = new Builder();

    final GraphQLDirective directive = graphQLArgument.getDirective("key");

    b.fields((String) directive.getArgument("fields").getValue());
    return b.build();

  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private String fields;

    private Builder() {
    }

    public Builder fields(final String val) {
      fields = Objects.requireNonNull(val);
      return this;
    }

    public KeyDirective build() {
      return new KeyDirective(this);
    }
  }
}
