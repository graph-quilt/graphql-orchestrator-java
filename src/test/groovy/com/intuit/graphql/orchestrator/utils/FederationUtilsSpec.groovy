package com.intuit.graphql.orchestrator.utils

import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.utils.FederationUtils.getUniqueIdFromFieldSet

class FederationUtilsSpec extends BaseIntegrationTestSpecification {
    void canCreateUniqueIdFromFieldSetWithoutChildren() {
        given:
        String fieldSet = "{ foo bar c1}"

        when:
        String id = getUniqueIdFromFieldSet(fieldSet)

        then:
        id == "barc1foo"
    }

    void canCreateUniqueIdFromFieldSetWithChildren() {
        given:
        String fieldSet = "{ foo bar c1 { d1 d2 d3 { e1 e2}}}"

        when:
        String id = getUniqueIdFromFieldSet(fieldSet)

        then:
        id == "barc1food1d2d3e1e2"
    }

    void reorderedFieldSetResultInSameUniqueId() {
        given:
        String fieldSet1 = "{ foo bar c1 { d1 d2 d3 { e1 e2 } } }"
        String fieldSet2 = "{ bar foo c1 { d2 d1 d3 { e2 e1 } } }"

        when:
        String id = getUniqueIdFromFieldSet(fieldSet1)
        String id2 = getUniqueIdFromFieldSet(fieldSet2)

        then:
        id == "barc1food1d2d3e1e2"
        id2 == id
    }
}
