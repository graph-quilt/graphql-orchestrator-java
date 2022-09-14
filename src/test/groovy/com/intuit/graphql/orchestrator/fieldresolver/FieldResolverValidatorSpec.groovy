package com.intuit.graphql.orchestrator.fieldresolver

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.intuit.graphql.graphQL.FieldDefinition
import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentNotAFieldOfParentException
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import spock.lang.Specification

class FieldResolverValidatorSpec extends Specification {
    FieldResolverContext fieldResolverContextMock

    Map<String, FieldDefinition> PARENT_TYPE_FIELDS

    def setup() {
        fieldResolverContextMock = Mock(FieldResolverContext)
        PARENT_TYPE_FIELDS = ImmutableMap.of("a", Mock(FieldDefinition.class))

        fieldResolverContextMock.getServiceNamespace() >> "TEST_NAMESPACE"
        fieldResolverContextMock.getParentTypename() >> "ParentType"
        fieldResolverContextMock.getFieldName() >> "fieldWithResolver"

        fieldResolverContextMock.getParentTypeFields() >> PARENT_TYPE_FIELDS
    }

    def "validate Required Fields, required Fields Not In Parent Type throws Exception"() {
        given:
        String expectedMessage =
                "'b' is not a field of parent type. serviceName=TEST_NAMESPACE, " +
                        "parentTypeName=ParentType, fieldName=fieldWithResolver"

        Set<String> requiredFields = ImmutableSet.of("b")

        fieldResolverContextMock.getParentTypeFields() >> PARENT_TYPE_FIELDS
        fieldResolverContextMock.getRequiredFields() >> requiredFields

        when:
        FieldResolverValidator.validateRequiredFields(fieldResolverContextMock)

        then:
        def exception = thrown(ResolverArgumentNotAFieldOfParentException)
        exception.getMessage().contains(expectedMessage)
    }

    def "validate Required Fields_required Fields Is Present In Parent Type_does Not Throw Exception"() {
        given:
        fieldResolverContextMock.getRequiredFields() >> ImmutableSet.of("a")

        when:
        FieldResolverValidator.validateRequiredFields(fieldResolverContextMock)

        then:
        noExceptionThrown()
    }

    def "validate Required Fields_no Required Fields_does Not Throw Exception"() {
        given:
        fieldResolverContextMock.getRequiredFields() >> Collections.emptySet()

        when:
        FieldResolverValidator.validateRequiredFields(fieldResolverContextMock)

        then:
        noExceptionThrown()
    }

    def "validate Required Fields_get Required Fields Is Null_does Not Throw Exception"() {
        given:
        fieldResolverContextMock.getRequiredFields() >> null

        when:
        FieldResolverValidator.validateRequiredFields(fieldResolverContextMock)

        then:
        noExceptionThrown()
    }
}
