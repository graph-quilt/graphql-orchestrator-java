package com.intuit.graphql.orchestrator.utils

import spock.lang.Specification

class FieldReferenceUtilSpec extends Specification {

    def "get All Field Reference from Emty String returns Empty Set"() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString("")

        expect:
        actual.isEmpty()
    }

    def "get All Field Reference from Null returns Empty Set"() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString(null)

        expect:
        actual.isEmpty()
    }

    def "get All Field Reference from Field Ref returns Extracted Fields"() {
        given:
        Set<String> actual = FieldReferenceUtil.getAllFieldReferenceFromString('$someFieldRef')

        expect:
        actual.size() == 1
        actual.contains("someFieldRef")
    }

    def "get All Field Reference from Json String returns Extracted Fields"() {
        given:
        Set<String> actual = FieldReferenceUtil
                .getAllFieldReferenceFromString('{ "field": $someFieldRef }')

        expect:
        actual.size() == 1
        actual.contains("someFieldRef")
    }

}
