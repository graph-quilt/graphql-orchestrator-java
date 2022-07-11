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

    def setup() {
        typeDefinitionMock = Mock(TypeDefinition.class)
        fieldResolverContextMock = Mock(FieldResolverContext.class)

        subjectUnderTest = new TypeMetadata(typeDefinitionMock)
    }

    def "get Field Resolver Context with No Field Resolvers returns Null"() {
        given:
        subjectUnderTest.addFieldResolverContext(fieldResolverContextMock)

        when:
        def actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME)

        then:
        actual == null
    }

    def "get Field Resolver Context with Field Resolvers can return Object"() {
        given:
        fieldResolverContextMock.getFieldName() >> TEST_FIELD_NAME
        subjectUnderTest.addFieldResolverContext(fieldResolverContextMock)

        when:
        def actual = subjectUnderTest.getFieldResolverContext(TEST_FIELD_NAME)

        then:
        actual.is(fieldResolverContextMock)
    }

}
