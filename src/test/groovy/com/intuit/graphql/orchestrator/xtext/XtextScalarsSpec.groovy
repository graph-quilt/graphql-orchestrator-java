package com.intuit.graphql.orchestrator.xtext

import helpers.BaseIntegrationTestSpecification

import static com.intuit.graphql.orchestrator.xtext.XtextScalars.*

class XtextScalarsSpec extends BaseIntegrationTestSpecification {

    void testDoesNotReturnSameInstance() {
        expect:
        !newBigDecimalType().is(newBigDecimalType())
        !newBigIntType().is(newBigIntType())
        !newBooleanType().is(newBooleanType())
        !newByteType().is(newByteType())
        !newCharType().is(newCharType())
        !newFloatType().is(newFloatType())
        !newIntType().is(newIntType())
        !newIdType().is(newIdType())
        !newStringType().is(newStringType())
        !newLongType().is(newLongType())
        !newShortType().is(newShortType())
        !newFieldSetType().is(newFieldSetType())
    }

    void testStandardScalarMap() {
        expect:
        XtextScalars.STANDARD_SCALARS.size() == 12
    }
}
