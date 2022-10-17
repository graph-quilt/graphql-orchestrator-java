package com.intuit.graphql.orchestrator.fieldresolver

import com.intuit.graphql.orchestrator.resolverdirective.ResolverArgumentDefinition
import com.intuit.graphql.orchestrator.schema.transform.FieldResolverContext
import spock.lang.Specification

class FieldResolverArgumentExceptionSpec extends Specification {

    def "FieldResolverArgumentException gives correct error message" () {
        given:
        String expectedMessage = "testMessage  resolverArgumentName=null, resolverArgumentValue=null  fieldName=null,  parentTypeName=null,  resolverDirectiveDefinition=null, serviceNameSpace=null"
        ResolverArgumentDefinition mockResolverArgumentDefinition = Mock(ResolverArgumentDefinition.class)
        FieldResolverContext mockFieldResolverContext = Mock(FieldResolverContext.class)
        FieldResolverArgumentException fieldResolverArgumentException = new FieldResolverArgumentException("testMessage", mockResolverArgumentDefinition, mockFieldResolverContext)

        expect:
        fieldResolverArgumentException.message == expectedMessage

    }
}
