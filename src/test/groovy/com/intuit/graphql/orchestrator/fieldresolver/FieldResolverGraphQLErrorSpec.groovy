package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.orchestrator.resolverdirective.ResolverDirectiveDefinition
import graphql.ErrorType
import spock.lang.Specification

class FieldResolverGraphQLErrorSpec extends Specification {

    private String TEST_ERROR_MESSAGE = "testErrorMessage"
    private String TEST_SERVICE_NAME = "testServiceName"
    private String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    private String TEST_FIELD_NAME = "testFieldName"

    private ResolverDirectiveDefinition mockResolverDirectiveDefinition

    def "fieldResolverGraphQLError error message is correctly formed" () {
        given:
        mockResolverDirectiveDefinition = Mock(ResolverDirectiveDefinition.class)
        String expectedMessage = "testErrorMessage.  fieldName=testFieldName,  parentTypeName=testParentTypeName,  resolverDirectiveDefinition=Mock for type 'ResolverDirectiveDefinition' named 'mockResolverDirectiveDefinition', serviceNameSpace=testServiceName"
        FieldResolverGraphQLError fieldResolverGraphQLError = FieldResolverGraphQLError.builder()
                .fieldName(TEST_FIELD_NAME)
                .parentTypeName(TEST_PARENT_TYPE_NAME)
                .resolverDirectiveDefinition(mockResolverDirectiveDefinition)
                .serviceNameSpace(TEST_SERVICE_NAME)
                .errorMessage(TEST_ERROR_MESSAGE)
                .build()

        expect:
        fieldResolverGraphQLError.message == expectedMessage
        fieldResolverGraphQLError.errorType == ErrorType.ExecutionAborted
    }
}
