package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ResolverArgumentFieldNotInSchemaTest {

  @Test
  public void producesCorrectErrorMessage() {
    final ResolverArgumentFieldNotInSchema error = new ResolverArgumentFieldNotInSchema(
        "argName", new FieldContext("rootObject", "rootField"),
        new FieldContext("parentObject", "parentField"));

    Assertions.assertThat(error)
        .hasMessage(
            "Resolver argument 'argName' in 'rootObject:rootField': field 'parentField' in InputType 'parentObject' does not exist in schema.");
  }
}