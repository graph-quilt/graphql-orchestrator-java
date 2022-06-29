package com.intuit.graphql.orchestrator.schema

import com.intuit.graphql.graphQL.TypeDefinition
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import spock.lang.Specification
import spock.lang.Subject

class TypeMetadataSpec extends Specification {

    private static final String TEST_FIELD_NAME = "testField"

    @Subject
    TypeMetadata subjectUnderTest

    TypeDefinition typeDefinitionMock

    FieldResolverContext fieldResolverContextMock

    void setup() {
        typeDefinitionMock = Mock(TypeDefinition.class)
        fieldResolverContextMock = Mock(FieldResolverContext.class)

        subjectUnderTest = new TypeMetadata(typeDefinitionMock)
    }

    def "getFieldResolverContext with no FieldResolvers returns Null"() {
        when:
        subjectUnderTest.addFieldResolverContext(fieldResolverContextMock)
        def actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME)

        then:
        actual == null
    }

    def "getFieldResolverContext with FieldResolvers can return Object"() {
        when:
        fieldResolverContextMock.getFieldName() >> TEST_FIELD_NAME
        subjectUnderTest.addFieldResolverContext(fieldResolverContextMock)
        def actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME)

        then:
        actual == fieldResolverContextMock
    }

}
