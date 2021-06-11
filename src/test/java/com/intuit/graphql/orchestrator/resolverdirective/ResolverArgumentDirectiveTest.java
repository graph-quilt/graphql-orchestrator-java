package com.intuit.graphql.orchestrator.resolverdirective;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.orchestrator.TestHelper;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import org.junit.Test;

public class ResolverArgumentDirectiveTest {

  @Test
  public void createsFromGraphQLArgument() {
    String schema = "schema { query: Query } type Query { a(arg: Int @resolver(field: \"a.b.c\")): Int } "
        + "directive @resolver(field: String) on ARGUMENT_DEFINITION";
    final GraphQLArgument arg = TestHelper.schema(schema).getQueryType().getFieldDefinition("a")
        .getArgument("arg");
    final ResolverArgumentDirective result = ResolverArgumentDirective.fromGraphQLArgument(arg);

    assertThat(result.getArgumentName()).isEqualTo("arg");
    assertThat(result.getField()).isEqualTo("a.b.c");
    assertThat(result.getInputType()).isEqualTo(Scalars.GraphQLInt);
  }
}