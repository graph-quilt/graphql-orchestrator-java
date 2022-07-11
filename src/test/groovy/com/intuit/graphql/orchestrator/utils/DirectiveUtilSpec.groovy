package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class DirectiveUtilSpec extends Specification {

    def "build Deprecation Reason Null Input Test"() {
        expect:
        DirectivesUtil.buildDeprecationReason(null) == null
    }
}
