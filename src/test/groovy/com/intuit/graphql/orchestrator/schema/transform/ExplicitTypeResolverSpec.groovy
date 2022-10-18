package com.intuit.graphql.orchestrator.schema.transform

import graphql.TypeResolutionEnvironment
import graphql.schema.GraphQLObjectType
import spock.lang.Specification

class ExplicitTypeResolverSpec extends Specification {
    private ExplicitTypeResolver explicitTypeResolver
    def setup() {
        explicitTypeResolver = new ExplicitTypeResolver()
    }
    def "Explicit Type Resolver with no typename in the objectMap"() {
        given:
        Map<String, Object> objData = new HashMap<>()
        TypeResolutionEnvironment typeResolutionEnvironment = new TypeResolutionEnvironment(objData, new HashMap<String, Object>(), null, null, null, null)
        GraphQLObjectType graphQLObjectType = explicitTypeResolver.getType(typeResolutionEnvironment)

        expect:
        graphQLObjectType == null
    }
}
