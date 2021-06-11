package com.intuit.graphql.orchestrator.resolverdirective;

import com.intuit.graphql.orchestrator.xtext.FieldContext;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ResolverArgumentLeafTypeNotSameTest {

  @Test
  public void producesCorrectErrorMessageWithoutParentContext() {
    final ResolverArgumentLeafTypeNotSame error = new ResolverArgumentLeafTypeNotSame(
        "argName", new FieldContext("rootObject", "rootField"), "String", "ObjectType");

    Assertions.assertThat(error)
        .hasMessage(
            "Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' to be 'ObjectType'.");
  }

  @Test
  public void producesCorrectErrorMessageWithParentContext() {
    final ResolverArgumentLeafTypeNotSame error = new ResolverArgumentLeafTypeNotSame(
        "argName", new FieldContext("rootObject", "rootField"), new FieldContext("parentObject", "parentField"),
        "String", "Int");

    Assertions.assertThat(error)
        .hasMessage(
            "Resolver argument 'argName' in 'rootObject:rootField': Expected 'String' in 'parentObject:parentField' to be 'Int'.");
  }
}