package com.intuit.graphql.orchestrator.batch

import graphql.language.*
import helpers.BaseIntegrationTestSpecification

class VariableReferenceExtractorSpec extends BaseIntegrationTestSpecification {

    private VariableReference reference = VariableReference.newVariableReference()
            .name("test_reference")
            .build()

    private VariableReferenceExtractor extractor

    void setup() {
        extractor = new VariableReferenceExtractor()
    }

    void extractsVariableReferences() {
        when:
        extractor.captureVariableReferences(Collections.singletonList(reference))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    void extractsVariableReferenceInObject() {
        given:
        ObjectValue objectValue = ObjectValue.newObjectValue()
                .objectField(ObjectField.newObjectField().value(reference).name("test_object").build()).build()

        when:
        extractor.captureVariableReferences(Collections.singletonList(objectValue))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    void extractsVariableReferenceInArray() {
        given:
        ArrayValue arrayValue = ArrayValue.newArrayValue()
                .value(IntValue.newIntValue().value(BigInteger.ONE).build())
                .value(reference).build()

        when:
        extractor.captureVariableReferences(Collections.singletonList(arrayValue))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    void extractsNoVariableReferences() {
        given:
        ArrayValue arrayValue = ArrayValue.newArrayValue()
                .value(IntValue.newIntValue(BigInteger.ONE).build())
                .value(StringValue.newStringValue("test").build())
                .build()

        when:
        extractor.captureVariableReferences(Collections.singletonList(arrayValue))

        then:
        extractor.getVariableReferences().isEmpty()
    }

    void returnsDifferentReferences() {
        given:
        def one = extractor.getVariableReferences()
        def two = extractor.getVariableReferences()

        expect:
        !one.is(two)
    }
}