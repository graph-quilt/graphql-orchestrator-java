package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class QueryPathUtilsSpec extends Specification {

    def "can convert a list of fields to FQN format"() {
        when:
        String pathString = QueryPathUtils.pathListToFQN(Arrays.asList("a", "b", "c"));

        then:
        pathString == "a.b.c"
    }

}
