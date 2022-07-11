package com.intuit.graphql.orchestrator.batch

import graphql.language.*
import spock.lang.Specification

class VariableReferenceExtractorSpec extends Specification {

    private VariableReference reference = VariableReference.newVariableReference()
            .name("test_reference")
            .build()

    private VariableReferenceExtractor extractor

    def setup() {
        extractor = new VariableReferenceExtractor()
    }

    def "extracts Variable References"() {
        when:
        extractor.captureVariableReferences(Collections.singletonList(reference))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    def "extracts Variable Reference In Object"() {
        given:
        ObjectValue objectValue = ObjectValue.newObjectValue()
                .objectField(ObjectField.newObjectField().value(reference).name("test_object").build()).build()

        when:
        extractor.captureVariableReferences(Collections.singletonList(objectValue))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    def "extracts Variable Reference In Array"() {
        given:
        ArrayValue arrayValue = ArrayValue.newArrayValue()
                .value(IntValue.newIntValue().value(BigInteger.ONE).build())
                .value(reference).build()

        when:
        extractor.captureVariableReferences(Collections.singletonList(arrayValue))

        then:
        extractor.getVariableReferences().toArray() == [reference]
    }

    def "extracts No Variable References"() {
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

    def "returns Different References"() {
        given:
        def one = extractor.getVariableReferences()
        def two = extractor.getVariableReferences()

        expect:
        !one.is(two)
    }
}