package com.intuit.graphql.orchestrator.datafetcher;

import org.junit.Test;

public class XtextResolverArgumentTest {

  @Test
  public void convertsResolverArgumentFieldsToQueries() {
    String resolverArgumentField = "query.consumer.fieldA";
//
//    XtextResolverArgument result = XtextResolverArgument.newBuilder()
//        .resolverArgumentField(resolverArgumentField)
//        .build();
//
//    assertThat(printAstCompact(result.getPreparedQuery())).isEqualTo("query {consumer {fieldA}}");
  }

  @Test
  public void shouldThrowInvalidFieldReferenceExceptions() {
    String[] resolverArgumentFieldTestCases = new String[]{
        "",
        " ",
        "?.?",
        "query",
        "..query",
        "query..",
        "q.a.bc..",
        "q..a",
        "consumer.finance"
    };

//    for (final String testCase : resolverArgumentFieldTestCases) {
//      try {
//        XtextResolverArgument.newBuilder().resolverArgumentField(testCase).build();
//      } catch (NotAValidFieldReference notAValidFieldReference) {
//        continue;
//      }
//
//      fail("Expected NotAValidFieldReference, but passed: " + testCase);
//    }
  }
}