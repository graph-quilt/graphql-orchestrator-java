package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class DirectiveUtilSpec extends Specification {

    void buildDeprecationReasonNullInputTest() {
        expect:
        DirectivesUtil.buildDeprecationReason(null) == null
    }
}
