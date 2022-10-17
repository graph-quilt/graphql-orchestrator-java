package com.intuit.graphql.orchestrator.schema

import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import spock.lang.Specification

class RuntimeGraphSpec extends Specification {
    RuntimeGraph runtimeGraph

    def setup() {
        runtimeGraph = RuntimeGraph.newBuilder()
                .additionalTypes(new HashMap<String, GraphQLType>())
                .additionalDirectives(new HashSet<GraphQLDirective>())
                .operationMap(new HashMap<Operation, GraphQLObjectType>())
                .build()
    }

    def "runTimeGraph is formed"() {
        expect:
        runtimeGraph != null
        runtimeGraph.getAdditionalTypes().size() == 0
        runtimeGraph.getAddtionalDirectives().size() == 0
    }
}
