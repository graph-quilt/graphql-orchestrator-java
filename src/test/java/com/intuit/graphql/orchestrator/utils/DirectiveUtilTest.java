package com.intuit.graphql.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DirectiveUtilTest {

  @Test
  public void buildDeprecationReasonNullInputTest() {
    assertThat(DirectivesUtil.buildDeprecationReason(null)).isNull();
  }
}
