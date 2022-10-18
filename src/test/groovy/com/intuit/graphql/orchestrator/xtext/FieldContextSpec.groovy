package com.intuit.graphql.orchestrator.xtext

import org.apache.commons.lang3.StringUtils
import spock.lang.Specification

class FieldContextSpec extends Specification {

    private static final String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    private static final String TEST_FIELD_NAME = "testFieldName"

    private FieldContext fieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, TEST_FIELD_NAME)

    def "equals Return False For Null"() {
        given:
        boolean actual = fieldContext.equals(null)

        expect:
        !actual
    }

    def "equals Return False For Other Type"() {
        given:
        String someOtherObject = StringUtils.EMPTY
        boolean actual = fieldContext.equals(someOtherObject)

        expect:
        !actual
    }

    def "equals Return False For Different Field Name"() {
        given:
        FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, "anotherField")
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        !actual
    }

    def "equals Return False For Different Parent Type Name"() {
        given:
        FieldContext anotherFieldContext = new FieldContext("anotherParentType", TEST_FIELD_NAME)
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        !actual
    }

    def "equals Return True For Same Values"() {
        given:
        FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, TEST_FIELD_NAME)
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        actual
    }

    def "equals Return True For Same Object"() {
        given:
        boolean actual = fieldContext.equals(fieldContext)

        expect:
        actual
    }

    def "field co-ordinates sets the correct value for parent type and field name"() {
        expect:
        fieldContext.fieldCoordinates.typeName == fieldContext.parentType
        fieldContext.fieldCoordinates.fieldName == fieldContext.fieldName
    }
}
