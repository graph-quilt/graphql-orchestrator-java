package com.intuit.graphql.orchestrator.resolverdirective

import spock.lang.Specification

class ResolverArgumentNotAFieldOfParentExceptionSpec extends Specification {

    private final String TEST_RESOLVER_ARG_VALUE = "testResolverArgValue"
    private final String TEST_PARENT_TYPE_NAME = "testParentTypeName"
    private final String TEST_REQUIRED_FIELD_NAME = "testReqdFieldName"
    private final String TEST_SERVICE_NAME = "testServiceName"

    def "ResolverArgumentNotAFieldOfParentException with resolverArgValue and parentTypeName parameter forms a correct error message"() {
        given:
        String expectedMessage = "Resolver argument value testResolverArgValue should be a reference to a field in Parent Type testParentTypeName"
        ResolverArgumentNotAFieldOfParentException resolverArgumentNotAFieldOfParentException =
                new ResolverArgumentNotAFieldOfParentException(TEST_RESOLVER_ARG_VALUE, TEST_PARENT_TYPE_NAME)
        expect:
        resolverArgumentNotAFieldOfParentException.message == expectedMessage
    }

    def "ResolverArgumentNotAFieldOfParentException with reqdFieldName, serviceName, resolverArgValue and parentTypeName parameter forms a correct error message"() {
        given:
        String expectedMessage = "'testReqdFieldName' is not a field of parent type. serviceName=testServiceName, parentTypeName=testResolverArgValue, fieldName=testParentTypeName, "
        ResolverArgumentNotAFieldOfParentException resolverArgumentNotAFieldOfParentException =
                new ResolverArgumentNotAFieldOfParentException(TEST_REQUIRED_FIELD_NAME, TEST_SERVICE_NAME, TEST_RESOLVER_ARG_VALUE, TEST_PARENT_TYPE_NAME)
        expect:
        resolverArgumentNotAFieldOfParentException.message == expectedMessage
    }
}
