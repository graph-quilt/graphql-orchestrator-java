package com.intuit.graphql.orchestrator.datafetcher

import helpers.BaseIntegrationTestSpecification

class XtextResolverArgumentSpec extends BaseIntegrationTestSpecification {

    void convertsResolverArgumentFieldsToQueries() {
        given:
        String resolverArgumentField = "query.consumer.fieldA"
//
//    XtextResolverArgument result = XtextResolverArgument.newBuilder()
//        .resolverArgumentField(resolverArgumentField)
//        .build()
//
//    assertThat(printAstCompact(result.getPreparedQuery())).isEqualTo("query {consumer {fieldA}}")
    }

    void shouldThrowInvalidFieldReferenceExceptions() {
        given:
        String[] resolverArgumentFieldTestCases = [
                "",
                " ",
                "?.?",
                "query",
                "..query",
                "query..",
                "q.a.bc..",
                "q..a",
                "consumer.finance"
        ]

//    for (final String testCase : resolverArgumentFieldTestCases) {
//      try {
//        XtextResolverArgument.newBuilder().resolverArgumentField(testCase).build()
//      } catch (NotAValidFieldReference notAValidFieldReference) {
//        continue
//      }
//
//      fail("Expected NotAValidFieldReference, but passed: " + testCase)
//    }
    }
}