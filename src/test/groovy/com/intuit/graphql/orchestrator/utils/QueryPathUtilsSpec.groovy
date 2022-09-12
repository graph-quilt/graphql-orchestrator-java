package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class QueryPathUtilsSpec extends Specification {

    def "pathListToFQN"() {
        given:
         def pathString = QueryPathUtils.pathListToFQN(Arrays.asList("a", "b", "c"));

        expect:
        pathString == "a.b.c"
    }

}
