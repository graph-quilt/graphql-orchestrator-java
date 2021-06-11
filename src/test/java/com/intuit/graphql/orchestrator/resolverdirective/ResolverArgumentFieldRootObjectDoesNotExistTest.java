package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ResolverArgumentFieldRootObjectDoesNotExistTest {

  @Test
  public void producesCorrectErrorMessage() {
    final ResolverArgumentFieldRootObjectDoesNotExist error = new ResolverArgumentFieldRootObjectDoesNotExist(
        "argName", new FieldContext("rootObject", "rootField"), "tax");

    Assertions.assertThat(error)
        .hasMessage(
            "Resolver argument 'argName' in 'rootObject:rootField': field 'tax' does not exist in schema.");
  }
}