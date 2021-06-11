package com.intuit.graphql.orchestrator.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class OperationTest {

  @Test
  public void testOperationName() {
    assertThat(Operation.QUERY.getName()).isEqualTo("Query");
    assertThat(Operation.MUTATION.getName()).isEqualTo("Mutation");
    assertThat(Operation.SUBSCRIPTION.getName()).isEqualTo("Subscription");
  }

}
