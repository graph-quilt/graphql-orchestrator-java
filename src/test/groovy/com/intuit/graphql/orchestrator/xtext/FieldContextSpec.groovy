package com.intuit.graphql.orchestrator.xtext

import org.apache.commons.lang3.StringUtils
import spock.lang.Specification

class FieldContextSpec extends Specification {

    private static final String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    private static final String TEST_FIELD_NAME = "testFieldName"

    private FieldContext fieldContext = new FieldContext(TEST_PARENT_TYPE_NAME,TEST_FIELD_NAME)

    void equalsReturnFalseForNull() {
        given:
        boolean actual = fieldContext.equals(null)

        expect:
        !actual
    }

    void equalsReturnFalseForOtherType() {
        given:
        String someOtherObject = StringUtils.EMPTY
        boolean actual = fieldContext.equals(someOtherObject)

        expect:
        !actual
    }

    void equalsReturnFalseForDifferentFieldName() {
        given:
        FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, "anotherField")
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        !actual
    }

    void equalsReturnFalseForDifferentParentTypName() {
        given:
        FieldContext anotherFieldContext = new FieldContext("anotherParentType", TEST_FIELD_NAME)
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        !actual
    }

    void equalsReturnTrueForSameValues() {
        given:
        FieldContext anotherFieldContext = new FieldContext(TEST_PARENT_TYPE_NAME, TEST_FIELD_NAME)
        boolean actual = fieldContext.equals(anotherFieldContext)

        expect:
        actual
    }

    void equalsReturnTrueForSameObject() {
        given:
        boolean actual = fieldContext.equals(fieldContext)

        expect:
        actual
    }
}
