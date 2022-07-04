package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class FieldReferenceUtilSpec extends Specification {

    void getAllFieldReference_fromEmtyString_returnsEmptySet() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString("")

        expect:
        actual.isEmpty()
    }

    void getAllFieldReference_fromNull_returnsEmptySet() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString(null)

        expect:
        actual.isEmpty()
    }

    void getAllFieldReference_fromFieldRef_returnsExtractedFields() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString('$someFieldRef')

        expect:
        actual.size() == 1
        actual.contains("someFieldRef")
    }

    void getAllFieldReference_fromJsonString_returnsExtractedFields() {
        given:
        Set<String> actual = FieldReferenceUtil
                .getAllFieldReferenceFromString('{ "field": $someFieldRef }')

        expect:
        actual.size() == 1
        actual.contains("someFieldRef")
    }

}
