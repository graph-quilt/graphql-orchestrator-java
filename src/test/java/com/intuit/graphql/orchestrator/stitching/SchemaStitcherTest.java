package com.intuit.graphql.orchestrator.stitching;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class SchemaStitcherTest {

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBuilder() {
    final SchemaStitcher.Builder sfBuilder = SchemaStitcher.newBuilder();
    assertThatThrownBy(() -> sfBuilder.service(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> sfBuilder.services(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> sfBuilder.batchLoaderHooks(null)).isInstanceOf(NullPointerException.class);
  }
}
