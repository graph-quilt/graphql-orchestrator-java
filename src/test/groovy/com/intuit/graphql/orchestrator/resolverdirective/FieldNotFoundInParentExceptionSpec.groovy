package com.intuit.graphql.orchestrator.resolverdirective

import graphql.ErrorType
import spock.lang.Specification

class FieldNotFoundInParentExceptionSpec extends Specification {

    static final String TEST_FIELD_NAME = "testFieldName"
    static final String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    static final String TEST_SERVICE_NAMESPACE = "testServiceNamespace"

    private ResolverDirectiveDefinition mockResolverDirectiveDefinition

    def "can Create Field Not Found In Parent Exception"() {
        given:
        mockResolverDirectiveDefinition = Mock(ResolverDirectiveDefinition.class)

        FieldNotFoundInParentException.Builder builder = FieldNotFoundInParentException
                .builder()
                .serviceNameSpace(TEST_SERVICE_NAMESPACE)
                .fieldName(TEST_FIELD_NAME)
                .parentTypeName(TEST_PARENT_TYPE_NAME)
                .resolverDirectiveDefinition(mockResolverDirectiveDefinition)

        String expectedMessage = String.format('''Field not found in parent's resolved value.  fieldName=%s,  parentTypeName=%s,  resolverDirectiveDefinition=%s, serviceNameSpace=%s''',
                TEST_FIELD_NAME, TEST_PARENT_TYPE_NAME, mockResolverDirectiveDefinition, TEST_SERVICE_NAMESPACE)

        when:
        FieldNotFoundInParentException fieldNotFoundInParentException = builder.build()

        then:
        fieldNotFoundInParentException != null
        fieldNotFoundInParentException.getMessage() == expectedMessage
        fieldNotFoundInParentException.getErrorType() == ErrorType.ExecutionAborted
    }

}
