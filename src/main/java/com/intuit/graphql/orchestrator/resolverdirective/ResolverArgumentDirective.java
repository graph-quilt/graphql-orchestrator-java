package com.intuit.graphql.orchestrator.resolverdirective;

import graphql.language.StringValue;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLInputType;
import graphql.schema.InputValueWithState;
import java.util.Objects;
import lombok.Getter;

/**
 * This class holds information about the resolver argument directive without the need to parse it via Xtext or
 * GraphQL-Java.
 */
@Getter
public class ResolverArgumentDirective {

  private final String argumentName;
  private final GraphQLInputType inputType;

  /**
   * The field in the resolver argument {@code @resolver(field: '')}
   */
  private final String field;

  private ResolverArgumentDirective(final Builder builder) {
    argumentName = builder.argumentName;
    field = builder.field;
    inputType = builder.graphQLInputType;
  }

  public static ResolverArgumentDirective fromGraphQLArgument(GraphQLArgument graphQLArgument) {
    Builder b = new Builder();

    final GraphQLDirective directive = graphQLArgument.getDirective("resolver");

    b.argumentName(graphQLArgument.getName());
    b.graphQLInputType(graphQLArgument.getType());

    InputValueWithState inputValueWithState = directive.getArgument("field").getArgumentValue();
    if (inputValueWithState.isLiteral()) {
      StringValue stringValue = (StringValue) inputValueWithState.getValue();
      b.field(stringValue.getValue());
    } else {
      b.field((String) inputValueWithState.getValue());
    }
    return b.build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private String argumentName;
    private String field;
    private GraphQLInputType graphQLInputType;

    private Builder() {
    }

    public Builder graphQLInputType(GraphQLInputType val) {
      this.graphQLInputType = val;
      return this;
    }

    public Builder argumentName(final String val) {
      this.argumentName = Objects.requireNonNull(val);
      return this;
    }

    public Builder field(final String val) {
      field = Objects.requireNonNull(val);
      return this;
    }

    public ResolverArgumentDirective build() {
      return new ResolverArgumentDirective(this);
    }
  }
}
