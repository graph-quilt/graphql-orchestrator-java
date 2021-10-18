package com.intuit.graphql.orchestrator.utils;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Arrays;
import org.junit.Test;

public class QueryPathUtilsTest {

  @Test
  public void pathListToFQN(){
    String pathString = QueryPathUtils.pathListToFQN(Arrays.asList("a","b","c"));
    assertThat(pathString).isEqualTo("a.b.c");
  }

}
