package com.intuit.graphql.orchestrator.xtext;

import static com.intuit.graphql.orchestrator.xtext.XtextScalars.newFieldSetType;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class XtextScalarsTest {

  @Test
  public void testDoesNotReturnSameInstance() {
    assertThat(newFieldSetType()).isNotSameAs(newFieldSetType());
  }

  @Test
  public void testStandardScalarMap() {
    assertThat(XtextScalars.STANDARD_SCALARS).hasSize(12);
  }
}