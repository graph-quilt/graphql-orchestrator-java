package com.intuit.graphql.orchestrator.resolverdirective;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.graphql.orchestrator.xtext.FieldContext;
import org.junit.Test;

public class ResolverArgumentTypeMismatchTest {

  @Test
  public void errorMessageWithoutParentContext() {
    final ResolverArgumentTypeMismatch error = new ResolverArgumentTypeMismatch("argName",
        new FieldContext("parentType", "fieldName"), "String", "ObjectType");

    assertThat(error)
        .hasMessage("Resolver argument 'argName' in 'parentType:fieldName': Expected type 'String' to be 'ObjectType'.");
  }

  @Test
  public void errorMessageWithParentContext() {
    FieldContext rootContext = new FieldContext("rootType", "rootField");
    FieldContext fieldContext = new FieldContext("parentType", "parentField");

    final ResolverArgumentTypeMismatch error = new ResolverArgumentTypeMismatch("argName",
        rootContext, fieldContext, "String", "Int");

    assertThat(error).hasMessage(
        "Resolver argument 'argName' in 'rootType:rootField': Expected type 'String' in 'parentType:parentField' to be 'Int'.");
  }
}