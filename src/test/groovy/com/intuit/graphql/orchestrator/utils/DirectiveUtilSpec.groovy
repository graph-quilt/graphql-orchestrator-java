package com.intuit.graphql.orchestrator.utils

import helpers.BaseIntegrationTestSpecification

class DirectiveUtilSpec extends BaseIntegrationTestSpecification {

    void buildDeprecationReasonNullInputTest() {
        expect:
        DirectivesUtil.buildDeprecationReason(null) == null
    }
}
