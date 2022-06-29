package com.intuit.graphql.orchestrator.resolverdirective

import graphql.ErrorType
import spock.lang.Specification

class FieldValueIsNullInParentExceptionSpec extends Specification {

    static final String TEST_SERVICE_NAMESPACE = "testServiceNamespace"
    static final String TEST_FIELD_NAME = "testFieldName"
    static final String TEST_PARENT_TYPE_NAME = "testParentTypeName"

    void canCreateFieldNotFoundInParentException() {
        given:
        def mockResolverDirectiveDefinition = Mock(ResolverDirectiveDefinition.class)

        def builder = FieldValueIsNullInParentException
                .builder()
                .serviceNameSpace(TEST_SERVICE_NAMESPACE)
                .fieldName(TEST_FIELD_NAME)
                .parentTypeName(TEST_PARENT_TYPE_NAME)
                .resolverDirectiveDefinition(mockResolverDirectiveDefinition)

        def expectedMessage = String.format(
                "Field value not found in parent's resolved value. "
                        + " fieldName=%s, "
                        + " parentTypeName=%s, "
                        + " resolverDirectiveDefinition=%s,"
                        + " serviceNameSpace=%s",
                TEST_FIELD_NAME, TEST_PARENT_TYPE_NAME, mockResolverDirectiveDefinition, TEST_SERVICE_NAMESPACE)

        when:
        FieldValueIsNullInParentException fieldValueIsNullInParentException = builder.build()

        then:
        fieldValueIsNullInParentException != null
        fieldValueIsNullInParentException.getMessage() == expectedMessage
        fieldValueIsNullInParentException.getErrorType() == ErrorType.ExecutionAborted
    }

}
