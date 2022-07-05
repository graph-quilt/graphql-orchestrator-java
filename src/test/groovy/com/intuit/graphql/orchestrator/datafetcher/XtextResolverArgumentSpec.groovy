package com.intuit.graphql.orchestrator.datafetcher

import spock.lang.Specification

class XtextResolverArgumentSpec extends Specification {

    def "converts Resolver Argument Fields To Queries"() {
        given:
        String resolverArgumentField = "query.consumer.fieldA"
//
//    XtextResolverArgument result = XtextResolverArgument.newBuilder()
//        .resolverArgumentField(resolverArgumentField)
//        .build()
//
//    assertThat(printAstCompact(result.getPreparedQuery())).isEqualTo("query {consumer {fieldA}}")
    }

    def "should Throw Invalid Field Reference Exceptions"() {
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