package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ResolverArgumentPrematureLeafTypeTest {

  @Test
  public void producesCorrectErrorMessage() {
    final ResolverArgumentPrematureLeafType error = new ResolverArgumentPrematureLeafType(
        "argName", "enumType", new FieldContext("rootObject", "rootField"), "tax");

    Assertions.assertThat(error)
        .hasMessage("Resolver argument 'argName' in 'rootObject:rootField': Premature enumType found in field 'tax'.");
  }
}